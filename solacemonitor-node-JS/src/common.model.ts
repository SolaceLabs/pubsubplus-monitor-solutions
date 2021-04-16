export interface Command {
  interval: number;
  type: string;
  cmdStr: string;
}

export interface PollingInstruction {
  description: string;
  cmdFile: string;
  configFile?: {
    fileName: string;
    columns: string[];
  };
  filenameTemplate: string;
  cmdList?: Command[];
  objList?: string[][];
  enabled: boolean;
}

export interface PollingTask {
  cmdStr: string;
  config?: {
    columns: string[];
    obj: string[];
  };
  filename: string;
  routerInfo: SolaceHostInfo;
}

export interface SolaceHostInfo {
  url: string;
  username: string;
  pwd: string;
  sempVer?: string;
}

interface InfluxDBInfo {
  db: string;
  username: string;
  pwd: string;
  host: string;
  continuousQuery: {
    duration: string,
    rpd: string,
    rp: string,
    adminUser: string,
    adminPassword: string
  }
}
export interface SystemConfiguration {
  solaceHosts: SolaceHostInfo[];
  influxDB: InfluxDBInfo;
  interface_stat: {
    networkInterface: string;
  }
}