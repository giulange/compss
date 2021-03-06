/*
 *  Copyright 2002-2015 Barcelona Supercomputing Center (www.bsc.es)
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
#include<iostream>
#include<fstream>
#include<string>
#include"TestMultiThread.h"
#include<pthread.h>

using namespace std;

#define FILE_NAME1 "counter1"
#define FILE_NAME2 "counter2"

void usage() {
    cerr << "[ERROR] Bad numnber of parameters" << endl;
    cout << "    Usage: simple <counterValue>" << endl;
}



void *thread_function1(void* unused){
	cout << "start func 1" << endl;
	string initialValue = "1";
	file fileName1 = strdup(FILE_NAME1);
	cout << "file 1 created" << endl;
    // Write file
    ofstream fos;
    //ofstream fos (fileName1,std::ofstream::out);
    cout << "before ofstream 1" << endl;
    compss_ofstream(fileName1, fos);
    cout << "after ofstream" << endl;
    if (fos.is_open()) {
        fos << initialValue << endl;
        fos.close();
    } else {
        cerr << "[ERROR] Unable to open file" << endl;
        return 0;
    }
    cout << "Initial counter1 value is " << initialValue << endl;

    // Execute increment
    increment(fileName1);
	cout << "function 1 called" << endl;
    // Read new value
    string finalValue;
    ifstream fis;
    compss_ifstream(fileName1, fis);
    if (fis.is_open()) {
        if (getline(fis, finalValue)) {
            cout << "Final counter1 value is " << finalValue << endl;
            fis.close();
        } else {
            cerr << "[ERROR] Unable to read final value" << endl;
            fis.close();
            return 0;
        }
    } else {
        cerr << "[ERROR] Unable to open file" << endl;
        return 0;
    }
    
    return 0;

}



void *thread_function2(void* unused){

	cout << "start func 2" << endl;

	string initialValue = "2";
	file fileName2 = strdup(FILE_NAME2);

	cout << "file 2 created" << endl;

    // Write file
    ofstream fos;
	cout << "before ofstream 2" << endl;
    //ofstream fos (fileName2,std::ofstream::out);
    compss_ofstream(fileName2, fos);
	cout << "after ofstream" << endl;
    if (fos.is_open()) {
        fos << initialValue << endl;
        fos.close();
    } else {
        cerr << "[ERROR] Unable to open file" << endl;
        return 0;
    }
    cout << "Initial counter2 value is " << initialValue << endl;	

    // Execute increment
    increment(fileName2);

	cout << "function 2 called" << endl;

    // Read new value
    string finalValue;
    ifstream fis;
    compss_ifstream(fileName2, fis);
    if (fis.is_open()) {
        if (getline(fis, finalValue)) {
            cout << "Final counter2 value is " << finalValue << endl;
            fis.close();
        } else {
            cerr << "[ERROR] Unable to read final value" << endl;
            fis.close();
            return 0;
        }
    } else {
        cerr << "[ERROR] Unable to open file" << endl;
        return 0;
    }
    return 0;

}





int main(int argc, char *argv[]) {
    // Check and get parameters
    if (argc != 2) {
        usage();
        return -1;
    }
//    string initialValue = argv[1];

	pthread_t thread1, thread2;

    // Init compss
    compss_on();
    
	cout << "runtime on" << endl;
        //void * a = thread_function2(NULL);
	pthread_create(&thread1, NULL, thread_function1, NULL);
	pthread_create(&thread2, NULL, thread_function2, NULL);

	cout << "threads created" << endl;

	pthread_join(thread1, NULL);
	pthread_join(thread2, NULL);

	cout << "joined" << endl;

    // Close COMPSs and end
    compss_off();
    return 0;
}
