package testTaskGroups;

import java.io.File;
import java.io.IOException;

import es.bsc.compss.api.COMPSs;
import es.bsc.compss.api.COMPSsGroup;
import es.bsc.compss.worker.COMPSsException;

public class TestTaskGroups {
    
    public static final int N = 3;
    public static final int M = 4;
    public static final int MAX_AVAILABLE = 1;

    public static final String FILE_NAME = "/tmp/sharedDisk/taskGroups.txt";
    
    public static void main(String[] args) throws Exception {
        newFile(FILE_NAME, true);
        
        System.out.println("[LOG] Test task groups");
        testTaskGroups();
        
        System.out.println("[LOG] Test task group exceptions with explicit barrier");
        testTaskGroupsBarrier();
        
        System.out.println("[LOG] Test task group exceptions");
        testGroupExceptions();
        
        System.out.println("[LOG] Test task group exceptions");
        testGroupExceptionsBarrier();
        
        COMPSs.getFile(FILE_NAME);
        
        System.out.println("[LOG] Test on failure ignore");
        testIgnoreFailure();
        
        System.out.println("[LOG] Test task time out");
        testTaskTimeOut();
    }
    
    private static void testIgnoreFailure() throws Exception {
        System.out.println("Executing write One ");
        TestTaskGroupsImpl.writeOnFailure(FILE_NAME);
        
        TestTaskGroupsImpl.writeFour(FILE_NAME);
    }

    private static void testGroupExceptions() throws InterruptedException {
        try (COMPSsGroup a = new COMPSsGroup("FailedGroup", true)){
            System.out.println("Executing write One ");
            for (int j=0; j<N; j++) {
                TestTaskGroupsImpl.writeOne(FILE_NAME);
           }
        }catch (COMPSsException e) {
            TestTaskGroupsImpl.writeThree(FILE_NAME);
            System.out.println("Exception caught!!");

        }catch (Exception e1) {
            e1.printStackTrace();
        } finally {
            TestTaskGroupsImpl.writeFour(FILE_NAME);
        }
    }

    private static void testGroupExceptionsBarrier() throws InterruptedException {
        try (COMPSsGroup a = new COMPSsGroup("FailedGroup2", false)){
            System.out.println("Executing write One ");
            for (int j=0; j<N; j++) {
                TestTaskGroupsImpl.writeOne(FILE_NAME);
           }
        } catch (Exception e) {
            e.printStackTrace();
        } 
        //The group exception will be thrown from the barrier
        try {
            COMPSs.barrierGroup("FailedGroup2");
        }catch (COMPSsException e) {
            System.out.println("Exception caught in barrier!!");
            TestTaskGroupsImpl.writeThree(FILE_NAME);
        } finally {
            TestTaskGroupsImpl.writeFour(FILE_NAME);
        }
    }
    
    private static void testTaskGroups() throws Exception{
        // Check of nested groups
        try (COMPSsGroup group1 = new COMPSsGroup("BigGroup") ) {
            // Create several nested groups containing writing tasks
            for (int i=0; i<M; i++) {
                try (COMPSsGroup n = new COMPSsGroup("group"+i)){
                    for (int j=0; j<N; j++) {
                         TestTaskGroupsImpl.writeTwo(FILE_NAME);
                    }
                }
            }
        } 
        
        // Creation of individual group of M tasks
        try (COMPSsGroup group = new COMPSsGroup("SmallGroup", true)) {
            for (int i=0; i < M; i++) {
                TestTaskGroupsImpl.writeTwo(FILE_NAME);
            }
        }
    }
    
    private static void testTaskGroupsBarrier() throws Exception{
        // Check of nested groups
        try (COMPSsGroup group1 = new COMPSsGroup("BigGroup2", false) ) {
            // Create several nested groups containing writing tasks
            for (int i=4; i<M*2; i++) {
                try (COMPSsGroup n = new COMPSsGroup("group"+i, false)){
                    for (int j=0; j<N; j++) {
                         TestTaskGroupsImpl.writeTwo(FILE_NAME);
                    }
                }
            }
        } 

        // Perform a barrier for every created group
        for (int i=4; i<M*2; i++) {
            COMPSs.barrierGroup("group"+i);
        }
        
        // Creation of individual group of M tasks
        try (COMPSsGroup group = new COMPSsGroup("SmallGroup2", true)) {
            for (int i=0; i < M; i++) {
                TestTaskGroupsImpl.writeTwo(FILE_NAME);
            }
        }
    }
    
    // Two tasks to check time out. The second takes more time than expected
    private static void testTaskTimeOut() throws Exception {
        TestTaskGroupsImpl.timeOutTaskFast(FILE_NAME);
        TestTaskGroupsImpl.timeOutTaskSlow(FILE_NAME);
    }
    
    // Creation of a new blank file
    private static void newFile(String fileName, boolean create) throws IOException {
        File file = new File(fileName);
        // Delete previous occurrences of the file
        if (file.exists()) {
            file.delete();
        }
        if (create) {
            // Create the file and directories if required
            boolean createdFile = file.createNewFile();
            if (!createdFile) {
                throw new IOException("[ERROR] Cannot create test file");
            }
        }

    }
}
