import path from "path";
import fs from "fs";
import xmlConvertor from "xml-js";
import jp from "jsonpath";
import request from "request";
import winston from "winston";
import http from "http";
import { Command, PollingInstruction, SystemConfiguration, SolaceHostInfo, PollingTask } from "./common.model"; 


const cmdDir = "./cmd";
const configDir = "./config";
const systemConfig: SystemConfiguration = require("./config/config.json");
const pollingInstructions = require("./cmd/pollingInstructions.json");

const SILLY = "silly";
const DEBUG = "debug";
const VERBOSE = "verbose";
const INFO = "info";
const WARN = "warn";
const ERROR = "error";


let logLevel = INFO;
let testFlag = false;

let routerHosts: SolaceHostInfo[] = [];


let myArgs = process.argv.slice(2);
if (myArgs.length < 1) {
  console.log("Usage: node pollrouter.js [-D|--DEBUG] <interval_queue>");
  process.exit(-1);
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
const intervalSecStr: string = myArgs.shift() || "";
const intervalSec: number = parseInt(intervalSecStr);
if (Number.isNaN(intervalSec)) {
  console.log("Invalid interval specified: " + intervalSecStr);
  process.exit(-2); 
}
//const scanDirectory = path.join("processing", intervalSecStr);
//const archiveDirectory = "store";
//const intermediateDirectory = "intermediate";

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
    new winston.transports.File({ filename: "log/pollrouter.js." + intervalSec + "_sec.log", maxsize: 20000000, maxFiles: 5 , tailable: true })
  ]
})

const startTime: number = Date.now();
const startTimeNano: bigint = BigInt(startTime) * 1000000n;
const startTimeHR: [number, number] = process.hrtime();

const keepAliveAgent = new http.Agent({keepAlive: true, maxSockets: 3});

function getCurTimeNano(): bigint {
  let timeDiffHR = process.hrtime(startTimeHR);

  return startTimeNano + BigInt(timeDiffHR[0]) * 1000000000n + BigInt(timeDiffHR[1]);
}

main();

async function main() {
  let curFuncName = "main";
  
  logger.info("Starting up... ", {label: curFuncName});
  logger.info("Querying routers for router-name...", {label: curFuncName});

  let routerNames: string[] = await getRouternames();
  let idx = 0;
  routerNames.forEach(curRouterName => {
    if (curRouterName.length > 0) {
      routerHosts[<any>curRouterName] = systemConfig.solaceHosts[idx];
    } else {
      logger.warn("Can't get the router name for [" + systemConfig.solaceHosts[idx].url + "], the router would be skipped");
    }
    idx++;
  });
  
  logger.debug("All router hosts info: ", {label: curFuncName});
  logger.debug(routerHosts.toString(), {label: curFuncName});

  getCmdFiles();
  getListFiles();
  logger.info("Start processing...", {label: curFuncName});
  processSingleCycle(routerNames);

}

function processSingleCycle(routerNames: string[]) {
  let curFuncName = "processSingleCycle";
  let processingStartTime = Date.now();

  pollingInstructions.instructions.
    filter((element: PollingInstruction) => element.enabled && element.cmdList !== undefined && element.cmdList.length > 0).
      forEach((curInstruction: PollingInstruction) => {
        logger.info("processing instruction with description [" + curInstruction.description + "]", {label: curFuncName});
        if (curInstruction.cmdList !== undefined) {
          curInstruction.cmdList.forEach(curCmd => {
            logger.info("Processing command type [" + curCmd.type + "]...", {label: curFuncName});
            if (curInstruction.configFile === undefined) {
              for (let curHostName in routerHosts) {
                let curCmdStr = replaceSempVer(curCmd.cmdStr, routerHosts[curHostName]);
                let curFilename = replaceVarInFilename(curInstruction.filenameTemplate, [ "hostname" ], [ curHostName ], curCmd.type);
                processPollingTask({
                  cmdStr: curCmdStr,
                  filename: curFilename,
                  routerInfo: routerHosts[curHostName]
                }, 1);
              }
            } else {
              if (curInstruction.objList === undefined || curInstruction.objList.length === 0) { 
                logger.debug("Object list from [" + curInstruction.configFile.fileName + "] is empty, skipped processing...", {label: curFuncName});
              } else {
                let cacheList: number[] = [];
                curInstruction.objList.forEach(curObj => {
                  if (curObj[0] !== undefined && curInstruction.configFile !== undefined) {
                    let curCmdStr = replaceCmdVariables(
                      replaceSempVer(curCmd.cmdStr,
                        routerHosts[<any>curObj[0]]),
                        curInstruction.configFile.columns, curObj);
                    if (isDuplicated(cacheList, curObj[0] + curCmdStr)) {
                      logger.info("Same SEMP request found in the same cycle, would be skipped", {label: curFuncName});
                      logger.debug(curCmdStr, {label: curFuncName});
                    } else {
                      let curFilename = replaceVarInFilename(curInstruction.filenameTemplate, curInstruction.configFile.columns, curObj, curCmd.type);
                      processPollingTask({
                        cmdStr: curCmdStr,
                        filename: curFilename,
                        routerInfo: routerHosts[<any>curObj[0]]
                      }, 1);
                    }
                  } else {
                    if (curObj[0] === undefined)
                      logger.warn("current object doesn't contain anything!", {label: curFuncName});
                    if (curInstruction.configFile === undefined)
                      logger.warn("config file info absent in the current instruction", {label: curFuncName});
                  }
                });
              }
            }
          });
        } else {
          logger.warn("no command list found", {label: curFuncName});
        }

      }
    );
  
  // calculate the time spent, from there, calculate the time for next run
  let processingEndTime = Date.now();
  let timeRemainingMs = intervalSec * 1000 - (processingEndTime - processingStartTime);

  if (timeRemainingMs > 0) {
    logger.info("scheduled to run the next round in " + timeRemainingMs + " ms", {label: curFuncName});
    setTimeout(processSingleCycle, timeRemainingMs, routerNames);
  } else {
    logger.info("running the next round immediately because the last run is overrun by " + (-timeRemainingMs) + " ms", {label: curFuncName});
    setImmediate(processSingleCycle, routerNames);
  }

  if (testFlag) {
    console.log("Completed 1 cycle.");
    console.log("Next cycle scheduled in " + timeRemainingMs + " ms");
  }
}

function isDuplicated(cacheList: number[], str: string): boolean {
  let curValue = hashCode(str);

  for (let i = 0; i < cacheList.length; i++) {
    if (cacheList[i] === curValue) {
      return true;
    }
  }

  cacheList.push(curValue);
  return false;
}
function processPollingTask(aTask: PollingTask, iter: number) {
  let curFuncName = "processPollingTask";
  let curFilename = aTask.filename.replace("$time", getCurTimeNano().toString());
  
  logger.info("Querying router at " + aTask.routerInfo.url + "...", {label: curFuncName});
  logger.debug("with request:", {label: curFuncName});
  logger.debug(aTask.cmdStr, {label: curFuncName});

  queryRouter(aTask.cmdStr, aTask.routerInfo.url, aTask.routerInfo.username,
    aTask.routerInfo.pwd).then(response => {
      writeToFile(response, getFullFilePath(curFilename.replace('$iter', iter.toString())));
      
      // processing "get more" if there's any
      if (response.includes("more-cookie")) {
        aTask.cmdStr = getNextRequest(response);
        processPollingTask(aTask, iter + 1);
      }
    }, reason => {
      logger.error("Failed to get the reply for task " + aTask, {label: curFuncName});
      logger.error(reason, {label: curFuncName});
    });
}

function getNextRequest(response: string): string {
  let requestText = "";
  let startPos = response.search("<more-cookie>");
  let endPos = response.search("</more-cookie>");
  requestText = response.substring(startPos + 13, endPos).trim();

  return requestText;
}

function getFullFilePath(filename: string): string {
  return path.join("processing", intervalSecStr, filename);
}

function writeToFile(content:string, filename: string) {
  let curFuncName = "writeToFile";
  logger.info("Writing to file " + filename, {label: curFuncName});

  fs.writeFile(filename, content, (err) => {
    if (err != null) {
      logger.error("Error writing to " + filename, {label: curFuncName});
      logger.error(err.toString(), {label: curFuncName});
    }
  });
}

function replaceVarInFilename(filename: string, columnList: string[], fieldList: string[], cmdType: string): string {
  let resultStr = filename;
  for (let idx = 0; idx < columnList.length; idx++) {
    resultStr = resultStr.replace("$" + columnList[idx], escapeCharactersInFilename(fieldList[idx]));
  }
  resultStr = resultStr.replace("$interval", intervalSecStr).replace("$type", cmdType);

  return resultStr;
}

function escapeCharactersInFilename(theStr: string): string {
  return theStr.replace(/(#|\\|\/|\+|>|<|\*)/g, x =>  ".." + x.charCodeAt(0).toString() + ".." );
}

function replaceSempVer(cmdStr: string, routerInfo: SolaceHostInfo): string {
  if (cmdStr.includes("SOLTRV") && routerInfo.sempVer != undefined) {
    return cmdStr.replace("SOLTRV", routerInfo.sempVer);
  } else {
    logger.debug("Either the command string doesn't require SEMP version or sempVer no available.",
    {label: replaceSempVer});
  }
  return cmdStr;
}

function replaceCmdVariables(cmdStr: string, columnList: string[], fieldList: string[]): string {
  let resultStr = cmdStr;
  for (let idx = 0; idx < columnList.length; idx++) {
    resultStr = resultStr.replace(columnList[idx].toUpperCase(), fieldList[idx]);
  }

  return resultStr;
}

function getCmdFiles() {
  let curFuncName = "getCmdFiles";
  
  logger.info("Retrieving cmd files...", {label: curFuncName});

  pollingInstructions.instructions.forEach((curInstruction: PollingInstruction) => {
    logger.info("Retrieving [" + curInstruction.description + "] with filename [" + curInstruction.cmdFile + "]", {label: curFuncName});
    try {
      let data: string = fs.readFileSync(path.join(cmdDir, curInstruction.cmdFile), "utf-8");
      let commandArr =
        data.split(/[\r\n]+/).
        filter((curLine) => curLine.trim().length > 0).
        map(parseCmdLine).
        filter((curCommand) => curCommand.interval == intervalSec);
      
      curInstruction.cmdList = commandArr;

      logger.debug("commands retrieved:", {label: curFuncName});
      logger.debug(commandArr.toString(), {label: curFuncName});
    } catch (e) {
      logger.error("Failed reading from " + curInstruction.cmdFile, {label: curFuncName});
      logger.error(e.toString(), {label: curFuncName});
    }
  });
}

function getListFiles() {
  let curFuncName = "getListFiles";
  logger.info("Retrieving object list files...", {label: curFuncName});

  pollingInstructions.instructions.forEach((curInstruction: PollingInstruction) => {
    logger.info("Processing object list for [" + curInstruction.description + "]", {label: curFuncName});
    if (curInstruction.cmdList !== undefined && curInstruction.cmdList.length > 0 && curInstruction.configFile !== undefined) {
      try {
        let data:string = fs.readFileSync(path.join(configDir, curInstruction.configFile.fileName), "utf-8");
        let objArr =
          data.split(/[\r\n]+/).
          filter((curLine) => curLine.trim().length > 0).map(parseListLine);
        
        logger.debug("Object list:", {label: curFuncName});
        logger.debug(objArr.toString(), {label: curFuncName});

        curInstruction.objList = objArr;
      } catch (e) {
        logger.error("Failed reading " + curInstruction.configFile.fileName, {label: curFuncName});
        logger.error(e, {label: curFuncName});
      }
    } else {
      if (curInstruction.cmdList === undefined || curInstruction.cmdList.length <= 0) {
        logger.info("Command list is empty!", {label: curFuncName});
      }
      if (curInstruction.configFile === undefined) {
        logger.info("Config file not defined!", {label: curFuncName});
      }
    }
  });
}



function parseCmdLine(line: string): Command {

  let lineArr = line.split(" ");
  let command: Command = {
    interval: parseInt(lineArr[1]),
    type: lineArr[0],
    cmdStr: line.substring(line.indexOf(lineArr[2]))
  }

  return command;
}

function parseListLine(line: string): string[] {

  let lineArr = line.split(/ +/);
  return lineArr;
}

function getRouternames(): Promise<string[]> {
  let promiseArr: Promise<string>[] = [];
  for (let curSolaceHost of systemConfig.solaceHosts) {
    promiseArr.push(getRoutername(curSolaceHost.url, curSolaceHost.username, curSolaceHost.pwd).catch(() => ""));
  }
  return Promise.all(promiseArr);
}

function getRoutername(hostUrl: string, username: string, pwd: string): Promise<string> {
  let curFuncName = "getRoutername";
  let requestBody = "<rpc> <show> <router-name></router-name> </show> </rpc>";
  return new Promise((resolve, reject) => queryRouter(requestBody, hostUrl, username, pwd).then((responseBody: string)=> {
    let replyjs: any = null;
    try {
      replyjs = xmlConvertor.xml2js(responseBody, { alwaysChildren: true, compact: true });
    }
    catch (e) {
      logger.error("Failed to convert xml response to JSON for : " + responseBody, {label: curFuncName});
      logger.error(e.message, {label: curFuncName});
      reject(e);
    }
    let elements = jp.query(replyjs, escapeSearchStr("$..show.router-name.router-name"));

    if (elements === undefined || elements === null) {
      logger.error("can't find router-name in the SEMP reply:", {label: curFuncName});
      logger.debug(responseBody, {label: curFuncName});
      reject("Failed getting the router-name");
    } else {
      resolve(elements[0]._text);
    }

  }).catch(
    (reason) => {
      logger.error("can't get router-name for " + hostUrl + "with reason: " + reason.toString(), {label: curFuncName});
      reject("getRoutername failed");
    }
  ));

}

function queryRouter(requestBody: string, hostUrl: string,
  username: string, password: string): Promise<string> {
  let curFuncName = "queryRouter";

  logger.debug("Request body: [" + requestBody + "] for router @ " + hostUrl, {label: curFuncName});
  
  return new Promise((resolve, reject) => {
      request({
        method: "POST",
        uri: "http://" + hostUrl + "/SEMP",
        body: requestBody,
        auth: {
          username: username,
          password: password
        },
        agent: keepAliveAgent,
        time: false
      }, (error, res, body) => {
        if (error != null || res.statusCode > 299 || res.statusCode < 200) {
          logger.error(`Failed to query the router!
          RouterURL: ${hostUrl}
          Record String: ${requestBody}
          Status Code: ${res ? res.statusCode : ""}
          Status Message: ${res ? res.statusMessage : ""}
          ${error ? error: ""}`, { label: curFuncName });
          reject("Failed to querying router");
        } else {
          resolve(body);
        }
      })
    }
  )

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

function hashCode(s: string): number {
  let h = 0;
  for(let i = 0; i < s.length; i++) 
    h = Math.imul(31, h) + s.charCodeAt(i) | 0;

  return h;
}
