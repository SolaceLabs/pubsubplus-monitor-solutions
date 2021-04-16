# Introduction
This is a custom Solace monitoring solution developed by Solace PSG in APAC. It was originally created as a set of bash scripts by Robert Hsieh. It was later further developed and ported to TypeScript/ Node.js for performance's sake by Vincent Lam.

It was later packaged as a Docker image ready to be deloyed in Docker or k8s environment.

For details on the design and setup, please refer to the User Guide (user_guide.md).

# Requirement
You need npm and tsc (TypeScript compiler) in order to compile the code (for obvious reasons). You also need Docker in order to create the Docker image (for obvious reasons).

Running the solution requires InfluxDB and Grafana to be set up as well. Please refer to the User Guide for details.

Of course, you also need running Solace PS+ instances as the monitored subjects as well.

# Installation
To compile source code (TypeScript), execute
```$ tsc -p ./tsconfig.json```

To build the docker image, execute
```$ docker build -t solacemonitor:2.0.0 .```

# Support
Contact Vincent Lam (vincent.lam@solace.com) for issues or questions about the solution.