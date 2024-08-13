# RULING

## Purpose

The _Ruling_ project is executing all the Apex rules against several Apex projects, controling that issues are correctly raised.
By default, tests are skipped. In order to execute the ruling tests, use one of the following command:

- Disabling the skipped tests option:

```
mvn clean install -DskipTests=False
```
- Or use the profile dedicated for Integration Tests: `its`
```
mvn clean install -P its
```

## Keep SonarQube alive

From time to time, it can be usefull to keep SonarQube alive at the end of the analysis on all the sources. It allows you to have a look at the issues which were unexpectedly raised (or not raised anymore) during analysis. Expected issues which are indeed raised during analysis are simply removed and won't show off. In order to do so, just use the following command:

```
mvn clean install -P its -DkeepSonarQubeRunning=True
```

Then, locate in your logs a line similar as this one:
```
INFO: ANALYSIS SUCCESSFUL, you can browse http://127.0.0.1:39811/dashboard?id=apex-project
```
It will give you the URL to access to visualize the results.

Closing the server can then be done with a simple `CTRL + C`.
