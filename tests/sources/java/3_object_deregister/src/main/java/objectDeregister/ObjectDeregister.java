package objectDeregister;

import es.bsc.compss.api.COMPSs;


public class ObjectDeregister {

    public static void main(String[] args) throws Exception {

        /*
         * This test provides a Dummy object used to provoke a situation where useless objects remain in memory until
         * the end of execution, this is creating a loop using the same object all over.
         */

        int k;
        final int ITERATIONS = 10;

        for (int i = 0; i < ITERATIONS; ++i) {
            Dummy d = new Dummy(i);

            ObjectDeregisterImpl.task1(i, d);
            ObjectDeregisterImpl.task2(i + 1, d);
            ObjectDeregisterImpl.task3(d);
            // Allows garbage collector to delete the object from memory
            COMPSs.deregisterObject((Object) d);
        }

        COMPSs.barrier();
        ObjectDeregisterImpl.task5();
        COMPSs.barrier();
        System.gc();
        Thread.sleep(10000);

        k = ClassInstanceTest.countInstances(Dummy.class);
        if (k > 0) {
            System.out.println(
                "[ERROR] At the end in the MASTER " + String.valueOf(k) + " instances of the Dummy object were found");
            System.exit(-1);
        }

        /*
         * task1 & task2 write into the object so a copy of the object will be created, the task3 will just read the
         * object task2 used
         */

        /*
         * This second part of the test checks that accesses from the main are properly handled.
         */
        // IN
        Dummy dIn = new Dummy(1);
        ObjectDeregisterImpl.task3(dIn);
        BlackBox.method(dIn);
        COMPSs.deregisterObject((Object) dIn);
        dIn = null;
        Thread.sleep(2000);
        COMPSs.barrier();
        System.gc();

        k = ClassInstanceTest.countInstances(Dummy.class);
        if (k > 0) {
            System.out.println(
                "[ERROR] At the end in the MASTER " + String.valueOf(k) + " instances of the Dummy object were found");
            System.exit(-1);
        }

        // OUT
        Dummy dOut = ObjectDeregisterImpl.task4(1);
        BlackBox.method(dOut);
        COMPSs.deregisterObject((Object) dOut);
        dOut = null;
        Thread.sleep(2000);
        COMPSs.barrier();
        System.gc();

        k = ClassInstanceTest.countInstances(Dummy.class);
        if (k > 0) {
            System.out.println(
                "[ERROR] At the end in the MASTER " + String.valueOf(k) + " instances of the Dummy object were found");
            System.exit(-1);
        }

        // INOUT
        Dummy dInout = new Dummy(2);
        ObjectDeregisterImpl.task1(2, dInout);
        BlackBox.method(dInout);
        COMPSs.deregisterObject((Object) dInout);
        dInout = null;
        Thread.sleep(2000);
        COMPSs.barrier();
        System.gc();

        k = ClassInstanceTest.countInstances(Dummy.class);
        if (k > 0) {
            System.out.println(
                "[ERROR] At the end in the MASTER " + String.valueOf(k) + " instances of the Dummy object were found");
            System.exit(-1);
        }
    }

}
