package statstics;

import appinfo.Application;

import appinfo.Executor;
import appinfo.Stage;
import util.FileTextWriter;
import util.Statistics;

import java.util.*;

/**
 * Created by xulijie on 17-7-3.
 */

public class ApplicationStatistics {

    private String appName;
    private Statistics duration;

    // each stage has its stage statistics
    // private Map<Integer, StageStatistics> stageStatisticsMap = new TreeMap<Integer, StageStatistics>();
    private Map<String, StageStatistics> stageStatisticsMap = new TreeMap<String, StageStatistics>();
    private ExecutorStatistics executorStatistics;

    // Successful without any failed stages/tasks
    private List<Application> successfulApps = new ArrayList<Application>();

    // Successful with failed stages/tasks
    private List<Application> finishedApps = new ArrayList<Application>();

    // Failed applications
    private List<Application> failedApps = new ArrayList<Application>();

    // Running applications
    private List<Application> runningApps = new ArrayList<Application>();

    // In general, we run  application 5 times, so the length of stageWithSameId is 5
    public ApplicationStatistics(List<Application> appsWithSameName) {
        // check if all the applications are completed
        for (Application app : appsWithSameName) {
            if (app.getStatus().equals("RUNNING")) {
                System.err.println("[WARN] The status of " + app.getName() + "-" + app.getAppId() + " is RUNNING");
                runningApps.add(app);
            } else if (app.getStatus().equals("FAILED")) {
                System.err.println("[WARN] The status of " + app.getName() + "-" + app.getAppId() + " is FAILED");
                failedApps.add(app);
            } else if (app.getStatus().equals("FINISHED")) {
                // has succeed and finished apps
                finishedApps.add(app);
                System.out.println("[INFO] The status of " + app.getName() + "-" + app.getAppId() + " is " + app.getStatus());
            } else if (app.getStatus().equals("SUCCEEDED")) {
                // has succeed and finished apps
                successfulApps.add(app);
                System.out.println("[INFO] The status of " + app.getName() + "-" + app.getAppId() + " is " + app.getStatus());
            }
        }

        computeAppStatistics();
        computeStageStatistics();
        computeExecutorStatistics();
    }

    private void computeAppStatistics() {
        Object[] appObjs = successfulApps.toArray();
        duration = new Statistics(appObjs, "getDuration");
    }

    /*
    private void computeStageStatistics() {

        // <stageId, [stage from app1, stage from app2, stage from appN]>
        Map<Integer, List<Stage>> stagesWithSameId = new TreeMap<Integer, List<Stage>>();

        List<Application> appList = new ArrayList<Application>();
        appList.addAll(completedApps);
        appList.addAll(failedApps);

        // also consider the complete stages in the failed apps
        for (Application app : appList) {
            for (Map.Entry<Integer, Stage> stageEntry : app.getStageMap().entrySet()) {
                int stageId = stageEntry.getKey();
                Stage stage = stageEntry.getValue();

                if (stagesWithSameId.containsKey(stageId)) {
                    stagesWithSameId.get(stageId).add(stage);
                } else {
                    List<Stage> stageList = new ArrayList<Stage>();
                    stageList.add(stage);
                    stagesWithSameId.put(stageId, stageList);
                }
            }
        }

        for (Map.Entry<Integer, List<Stage>> stagesEntry : stagesWithSameId.entrySet()) {
            StageStatistics stageStatistics = new StageStatistics(stagesEntry.getValue());
            stageStatisticsMap.put(stagesEntry.getKey(), stageStatistics);
        }
    }
    */

    private void computeStageStatistics() {

        // <stageId, [stage from app1, stage from app2, stage from appN]>
        Map<String, List<Stage>> stagesWithSameName = new TreeMap<String, List<Stage>>();

        List<Application> appList = new ArrayList<Application>();
        appList.addAll(successfulApps);
        appList.addAll(finishedApps);
        appList.addAll(failedApps);

        // also consider the complete stages in the failed apps
        for (Application app : appList) {
            for (Map.Entry<Integer, Stage> stageEntry : app.getStageMap().entrySet()) {
                int stageId = stageEntry.getKey();
                String stageName = stageEntry.getValue().getStageName();
                Stage stage = stageEntry.getValue();

                if (stagesWithSameName.containsKey(stageName)) {
                    stagesWithSameName.get(stageName).add(stage);
                } else {
                    List<Stage> stageList = new ArrayList<Stage>();
                    stageList.add(stage);
                    stagesWithSameName.put(stageName, stageList);
                }
            }
        }

        for (Map.Entry<String, List<Stage>> stagesEntry : stagesWithSameName.entrySet()) {
            StageStatistics stageStatistics = new StageStatistics(stagesEntry.getValue());
            stageStatisticsMap.put(stagesEntry.getKey(), stageStatistics);
        }
    }

    private void computeExecutorStatistics() {
        List<Executor> executorsMultipleApps = new ArrayList<Executor>();

        List<Application> appList = new ArrayList<Application>();
        appList.addAll(successfulApps);
        appList.addAll(finishedApps);
        appList.addAll(failedApps);

        for (Application app : appList) {
            List<Executor> executorsPerApp = new ArrayList<Executor>();

            if (app.getStatus().equals("SUCCEEDED")) {
                executorsPerApp = app.getExecutors();
            }
            else if (app.getStatus().equals("FINISHED")) {
                for (Executor executor : app.getExecutors())
                    if (executor.isActive() == false)
                        executorsPerApp.add(executor);
            } else if (app.getStatus().equals("FAILED")) {
                for (Executor executor : app.getExecutors())
                    if (executor.isActive() == false)
                        executorsPerApp.add(executor);
            }

            executorsMultipleApps.addAll(executorsPerApp);
        }

        executorStatistics = new ExecutorStatistics(executorsMultipleApps);
    }

    public String getAppName() {
        return appName;
    }



    public void setAppName(String appName) {
        this.appName = appName;
    }

    @Override
    public String toString(){
        StringBuilder sb = new StringBuilder();

        sb.append("\n");
        sb.append("===============================================================" + appName
                + "===============================================================\n");
        sb.append("[app.duration] " + duration + "\n");

        for (Map.Entry<String, StageStatistics> stageStatisticsEntry : stageStatisticsMap.entrySet()) {
            Set<Integer> stageId = stageStatisticsEntry.getValue().getStageId();
            StageStatistics stageStatistics = stageStatisticsEntry.getValue();

            sb.append("-------------------------------------------------------------------[Stage "
                    + stageId
                    + "]-------------------------------------------------------------------\n");
            sb.append(stageStatistics);
        }

        sb.append("-------------------------------------------------------------------------"
                + "[Executor Statistics]-------------------------------------------------------------------------\n");
        sb.append(executorStatistics);

        sb.append("=========================================================================="
                + "===============================================================\n");

        return sb.toString();
    }
}

