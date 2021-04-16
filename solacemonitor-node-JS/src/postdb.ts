import path from "path";
import fs from "fs";
import moveFile from "move-file"
import xmlConvertor from "xml-js";
import jp from "jsonpath";
import http from "http";
import request from "request";
const winston = require("winston"); 

const systemConfig = require("./config/config.json");
const processingInstructions = require("./cmd/processingInstructions.json");

const SILLY: string = "silly";
const DEBUG: string = "debug";
const VERBOSE: string = "verbose";
const INFO: string = "info";
const WARN: string = "warn";
const ERROR: string = "error";


let logLevel: string = INFO;
let testFlag: boolean = false;

let myArgs = process.argv.slice(2);
if (myArgs.length < 2) {
  console.log("Usage: node postdb.js [-D|--DEBUG] interval_queue file_matching");
  process.exit(1);
}

let isParseFinished: boolean = false;
while (!isParseFinished) {
  switch(myArgs[0]) {
    case "-D": 
    case "--DEBUG": {
      logLevel = DEBUG;
      myArgs.shift();
      break;
    }
    case "-T": 
    case "--TEST": {
      testFlag = true;
      myArgs.shift();
      break;
    }
    default: {
      isParseFinished = true;
      break;
    }
  }
}
const intervalSec: string = myArgs.shift() || "";
const scanDirectory = path.join("processing", intervalSec);
const archiveDirectory = "store";
const intermediateDirectory = "intermediate";

const myLogFormat = winston.format.printf((info: any) => {
  return `${info.timestamp} [${info.label}] ${info.level}: ${info.message}`;
})

const logger = winston.createLogger({
  level: logLevel,
  format: winston.format.combine(
    //winston.format.label({label: "postdb.js"}),
    winston.format.timestamp(),
    myLogFormat),
  transports: [
    new winston.transports.File({ filename: "log/postdb.js." + intervalSec + "_sec.log", maxsize: 20000000, maxFiles: 5 , tailable: true })
  ]
})

if (logLevel === DEBUG) {
  logger.info("Debug flag ON...", {label: "main"});
}
if (testFlag === true) {
  logger.info("Test flag ON, records won't be inserted into DB...", {label: "main"});
}

// parsing environment variables
var isArchiveResponses:boolean = true;
if (process.env.SMON_IS_ARCHIVE_RESPONSES === "FALSE") {
  isArchiveResponses = false;
  logger.info("SMON_IS_ARCHIVE_RESPONSES is set to FALSE, SEMP responses won't be archived", {label: "main"});
}

// create the keepAliveAgent for sending to InfluxDB
const keepAliveAgent = new http.Agent({keepAlive: true, maxSockets: 3});

// start processing
setImmediate(() => { processDirectory(scanDirectory, 0) });

function processDirectory(directory: string, errorCnt: number) {
  let files: string[];
  let timeOutMs: number = 1000;
  let curErrorCnt: number = 0;
  try {
    files = fs.readdirSync(scanDirectory);
    files.forEach((curFile: string) => {
      logger.info("processing " + curFile, {label: "proccessDirectory"});
      //fs.renameSync(path.join(scanDirectory, curFile), path.join(intermediateDirectory, curFile));
      moveFile.sync(path.join(scanDirectory, curFile), path.join(intermediateDirectory, curFile));
      if (testFlag) {
        process.stdout.write(".");
      }
      dispatchFile(curFile);
    });
    timeOutMs = (files.length === 0 ? 4000 : 1000);
  } catch (e) {
    logger.error("error reading directory", { label: "processDirectory" });
    logger.error(e, { label: "processDirectory" });
    timeOutMs = 4000;
    curErrorCnt++; // flag error
    //process.exit(-1);
  }

  if ((errorCnt + curErrorCnt) > 5) {
    // bail out if the process can't read the directory for 5 times consecutively
    // need to wait 2 sec for the Winston logger to be flushed before exiting
    setTimeout(() => { process.exit(-1) }, 2000);
  }

  // continue with the next directory scan
  setTimeout(() => {
    processDirectory(directory, (curErrorCnt === 0 ? 0 : curErrorCnt + errorCnt))
  }, timeOutMs);
}

function replaceVariables(recordStr: string, varList: { [key: string]: string; }) {
  let influxRecord = recordStr;
  for (let key in varList) {
    let searchRegex = new RegExp("<" + key + ">", "g");
    influxRecord = influxRecord.replace(searchRegex, varList[key]);
  }
  if (influxRecord.indexOf("<") >= 0) {
    logger.error("Error! Not all variables are replaced: [" + influxRecord +"]", { label: "replaceVariables"});
  } else {
    logger.debug("recordStr: [" + influxRecord + "]", { label: "replaceVariables"});
  }
  
  return influxRecord;
}

function sendRecordToDB(recordStr: string, fileName: string) {

  if (testFlag) {
    logger.info("Inserting record: [" + recordStr + "] for file: " + fileName, { label: "sendRecordToDB" });
    return;
  }
  
  let influxDBConfig = systemConfig.influxDB;
  request({
    method: "POST",
    uri: "http://" + influxDBConfig.host + "/write?db=" + influxDBConfig.db,
    body: recordStr,
    auth: {
      username: influxDBConfig.username,
      password: influxDBConfig.pwd
    },
    agent: keepAliveAgent,
    time: false
  }, (error, res, body) => {
    if (error != null || res.statusCode > 299 || res.statusCode < 200) {
      logger.error(`Failed to send record to InfluxDB!
      File: ${path.join(intermediateDirectory, fileName)}
      Record String: ${recordStr}
      Status Code: ${res ? res.statusCode : ""}
      Status Message: ${res ? res.statusMessage : ""}
      ${error ? error: ""}`, { label: "sendRecordToDB"});
    }
  })

}

function escapeSearchStr(searchStr: string): string {
  if (searchStr.indexOf("[") < 0 && searchStr.indexOf("-") >= 0) {
    searchStr = searchStr.replace(/([a-z\-]+)/g, '["$1"]');
    searchStr = searchStr.replace(/\.\./g, "^^");
    searchStr = searchStr.replace(/\./g, "");
    searchStr = searchStr.replace(/\^\^/g, "..");
  }
  return searchStr;
}

function replaceReservedWords(str: string, instruction: any): string {
  if (instruction["valueMapping"] != undefined) {
    if (instruction.valueMapping[str] != undefined) {
      return instruction.valueMapping[str];
    }
  }

  return str;
}

interface FileToBeProcessed {
  intervalSec: number;
  processType: string;
  solaceHost: string;
  timeStamp: string;
  fileName: string;
  remainingFileAttrs: string[];
  content: string;
  contentLength: number;
}

function dispatchFile(curFile: string) {
  let startTimeMs = (new Date()).getTime();
  let fileName: string = curFile.toString();
  let fileAttributes: string[] = fileName.split("+").map(convertFromEscapedFilename);

  if (fileAttributes.length > 0) {
    let buf: Buffer = fs.readFileSync(path.join(intermediateDirectory, curFile));
    let curFileObj: FileToBeProcessed = {
      intervalSec: parseInt(fileAttributes.shift() || ""),
      processType: fileAttributes.shift() || "",
      solaceHost: fileAttributes.shift() || "",
      timeStamp: fileAttributes.pop() || "",
      fileName: curFile,
      remainingFileAttrs: fileAttributes,
      content: buf.toString(),
      contentLength: buf.byteLength
    };

    processFile(curFileObj);
    
    let endTimeMs = (new Date).getTime();

    let curTimeStamp: number = (new Date()).getTime() * 1000000;
    sendRecordToDB(curFileObj.processType + ",timegap=" + curFileObj.intervalSec + ",hostname=" + curFileObj.solaceHost + " fsize=" + curFileObj.contentLength + ",value=" + (endTimeMs - startTimeMs) * 1000000 + " " + curTimeStamp, curFile);
    sendRecordToDB(curFileObj.processType + "-size,timegap=" + curFileObj.intervalSec + ",hostname=" + curFileObj.solaceHost + " diff=" + (endTimeMs - startTimeMs) * 1000000 + ",value=" + curFileObj.contentLength + " " + curTimeStamp, curFile);
    logger.info("Processing time in ms = " + (endTimeMs - startTimeMs), { label: "dispatchFile"});
    logger.info("File size in bytes: " + curFileObj.contentLength, { label: "dispatchFile" });

    //fs.renameSync(path.join(intermediateDirectory, curFile), path.join(archiveDirectory, curFile));
    if (isArchiveResponses === true) {
      moveFile.sync(path.join(intermediateDirectory, curFile), path.join(archiveDirectory, curFile));
    }
    else {
      // remove the file asynchronously if it's not needed to be archived
      fs.unlink(path.join(intermediateDirectory, curFile), (err) => {
        if (err != null) {
          logger.error("Error removing file [" + path.join(intermediateDirectory, curFile) + "]", { label: "dispatchFile" });
          logger.error(err, { label: "dispatchFile" });
        }
      })
    }
    
  } else {
    logger.error("Invalid file name: " + fileName, { label: "dispatchFile"});
  }
}

function processFile(curFile: FileToBeProcessed) {
  let replyjs: any = null;
  try {
    replyjs = xmlConvertor.xml2js(curFile.content, { alwaysChildren: true, compact: true });
  }
  catch (e) {
    logger.error("Failed to convert xml response to JSON for file: " + curFile.fileName, { label: "processFile" });
    logger.error(e.message, { label: "processFile" });
    return;
  }

  let instructions = jp.query(processingInstructions, '$..instructions[?(@.type=="' + curFile.processType + '")]');
  if (instructions.length > 0) {
    instructions.forEach((curInstruction) => {
      // compose variable
      let varList: {[key: string]: string} = {};
      varList["hostname"] = curFile.solaceHost;
      varList["timestamp"] = curFile.timeStamp;

      // Retrieve all elements specified in "iterateElement" in the current processing instruction.
      // An "iterateElement" could refer to message-vpn, cient-username, etc.
      // Most types of reply messages contain only one instance of the "iterateElement".
      // But in monitoring objects like "client-name", the show command would use "*" and the returned
      // reply messages would contain multiples
      logger.debug("iteration search str: " + escapeSearchStr(curInstruction.iterateElement), { label: "processFile" });
      let elementList = jp.query(replyjs, escapeSearchStr(curInstruction.iterateElement));
      if (elementList.length > 0) {
        while (Array.isArray(elementList[0])) {
          // needed since jp.query might return result as nested Array
          elementList = elementList[0];
        }
        elementList.forEach((curElement) => {
          curInstruction.variables.forEach((curVar: {name: string, value: string}) => {
            let value: string = "";
            if (curVar.value.startsWith("#")) {
              // #3, #4 syntax, referring to the position in the file attributes

              // search remainingFileAttrs for variable value
              let attrIdx: number = parseInt(curVar.value.substr(1));
              if (Number.isNaN(attrIdx)) {
                let fileAttrs: string[] = curFile.remainingFileAttrs;
                varList[curVar.name] = fileAttrs[attrIdx - 4];
              } else {
                logger.error("can't find file attribute with index: " + curVar.value, { label: "processFile" });
                return;
              }

            } else {
              // search response text for variable values
              let searchStr = escapeSearchStr(curVar.value);

              let foundElement = jp.value(curElement, searchStr);
              if (foundElement == undefined || foundElement == null) {
                logger.error("can't find element with search string: " + searchStr, { label: "processFile" });
              } else {
                value = replaceReservedWords(jp.value(curElement, searchStr)._text, curInstruction);
                varList[curVar.name] = value;
              }
            }

            logger.debug(curVar.name + ":" + value + " (" + curVar.value + ")", { label: "processFile" });
          });
          logger.debug("done composing varList", { label: "processFile" })

          // check skipCondition
          // If any skip condition is matched, the current element would be skipped and won't be processed.
          let skipConditions = curInstruction["skipConditions"];
          let shouldSkip: boolean = checkSkipCondition(skipConditions, varList);
          if (shouldSkip) {
            return;
          }
          
          // parseKeys refers to each metric that has to be parsed and send to the DB independently
          // Each of the parseKeys would be a measurement if we take InfluxDB as the target DB
          curInstruction.parseKeys.forEach((curKey: string) => {
            if (curKey.length > 0) {
              if (curKey.indexOf("##") <= 0) {
                // simple search
                // directly use the parseKey as the key to search for the corresponding value
                let searchStr = "";
                if (curInstruction.enclosingElement != null && curInstruction.enclosingElement.length > 0) {
                  if (curInstruction.enclosingElement.startsWith("$")) {
                    searchStr = curInstruction.enclosingElement;
                    if (searchStr.indexOf("[") > 0) {
                      searchStr += "[\"" + curKey + "\"]"
                    } else {
                      searchStr += "." + curKey;
                      searchStr = escapeSearchStr(searchStr);
                    }
                  } else {
                    searchStr = "$.." + curInstruction.enclosingElement + "." + curKey;
                    searchStr = escapeSearchStr(searchStr);
                  }
                } else {
                  searchStr = "$.." + curKey;
                  searchStr = escapeSearchStr(searchStr);
                }
                
                let foundElement = jp.value(curElement, searchStr);
                let foundValue: string = replaceReservedWords(foundElement._text, curInstruction);
                if (foundElement != undefined && foundValue != null) {
                  varList["value"] = foundValue;
                  varList["key"] = curKey;
                  let influxRecord = replaceVariables(curInstruction.recordStr, varList);
                  sendRecordToDB(influxRecord, curFile.fileName);
                } else {
                  logger.error("can't find any values with key [" + curKey + "] and search string: " + searchStr, { label: "processFile" });
                }
              } else {
                // content search
                // search for the element with a child having a particular value
                // 
                let searchStr = "$..";
                if (curInstruction.enclosingElement != null && curInstruction.enclosingElement.length > 0) {
                  searchStr += curInstruction.enclosingElement;
                } else {
                  logger.error("enclosingElement must exist for content search! Skipping [" + curKey + "]", { label: "processFile" });
                  return;
                }
                let searchKeyArray = curKey.split("##");
                if (searchKeyArray.length != 3) {
                  logger.error("parseKey not of the right format! Skipping [" + curKey + "]", { label: "processFile" });
                  return;
                }
                searchStr += "[?(@." + searchKeyArray[0] + "._text==\"" + searchKeyArray[1] + "\")]";
                let foundElement = jp.value(curElement, searchStr);
                if (foundElement != undefined) {
                  varList["key"] = searchKeyArray[1].replace(/ /g, "-");
                  varList["value"] = replaceReservedWords(foundElement[searchKeyArray[2]]._text, curInstruction);
                  let influxRecord = replaceVariables(curInstruction.recordStr, varList);
                  sendRecordToDB(influxRecord, curFile.fileName);
                } else {
                  logger.error("can't find any values with key [" + curKey + "] and search string: " + searchStr, { label: "processFile" });
                }

              }
            } else {
              // no parseKeys, it means the record doesn't need any
              // just compose the record using the varList is sufficient
              let influxRecord = replaceVariables(curInstruction.recordStr, varList);
              sendRecordToDB(influxRecord, curFile.fileName);
            }
          });

        });
      } else {
        logger.error("Can't find any element with path: " + curInstruction.iterateElement, { label: "processFile" });
      }
    });
  }
  else {
    logger.error("instruction not found for type: " + curFile.processType, { label: "processFile" });
  }
}

// If skip condition is matched, the current element would be skipped and won't be processed.
// Skip condition is based the variable list just built up above.
// Multiple skip condition would be treated as "OR".
function checkSkipCondition(skipConditions: Array<{
  "varName": string;
  "operator": string;
  "value": string;}
  >, varList: { [key: string]: string; }) {

  let funcName = "checkSkipCondition"; // for logger messages' label

  if (skipConditions != undefined && skipConditions.length > 0) {
    skipConditions.forEach((curCondition) => {
      let varValue = varList[curCondition.varName];
      if (varValue != undefined && curCondition["operator"] != undefined && curCondition["value"] != undefined) {
        switch (curCondition.operator) {
          case "==":
            if (varValue == curCondition.value) {
              logger.info(curCondition.varName + " == " + curCondition.value + " , skip processing", { label: funcName });
              return true;
            }
            break;
          case "!=":
            if (varValue != curCondition.value) {
              logger.info(curCondition.varName + " != " + curCondition.value + " , skip processing", { label: funcName });
              return true;
            }
            break;
          default:
            logger.error("Don't know how to processing operator [" + curCondition.operator + "]", { label: funcName });
        }
      }
      else {
        logger.error("Invalid skip condition: " + curCondition.toString() + " , continue processing.", { label: funcName });
      }
    });
  }
  return false;
}

function convertFromEscapedFilename(theStr: string): string {
  return theStr.replace(/\.\.[0-9]+\.\./g, x => String.fromCharCode(parseInt(x.substring(2, x.length - 2),10))); 
}