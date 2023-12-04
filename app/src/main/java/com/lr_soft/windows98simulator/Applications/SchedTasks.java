package com.lr_soft.windows98simulator.Applications;

import android.graphics.Bitmap;

import com.lr_soft.windows98simulator.R;

public class SchedTasks extends Explorer {
    public SchedTasks(){
        super("Scheduled Tasks", "Scheduled Tasks", R.drawable.directory_sched_tasks_0, R.drawable.directory_sched_tasks_2, false);
        addLink(new Link("Add Scheduled Task",
                "The Scheduled Task wizard walks you step-by-step through adding tasks. Just follow the instructions on each screen.",
                getBmp(R.drawable.template_sched_task_3), parent -> new Wizard("Scheduled Tasks Wizard", true,
                        new Bitmap[]{getBmp(R.drawable.sched_tasks_wiz1), getBmp(R.drawable.sched_tasks_wiz2), getBmp(R.drawable.sched_tasks_wiz3),
                                getBmp(R.drawable.sched_tasks_wiz4), getBmp(R.drawable.sched_tasks_wiz5)})));
        addLink(new Link("Tune-up Application Start",
                "Schedule: Multiple schedule times",
                getBmp(R.drawable.tune_up_app_start)));
        helpText = defaultHelpText = "This folder contains tasks you've scheduled for Windows. Windows automatically performs each task at the scheduled time. For example, you can schedule a time for Windows to clean up your hard disk by deleting unnecessary files.";
        linkContainer.updateLinkPositions();
    }
}
