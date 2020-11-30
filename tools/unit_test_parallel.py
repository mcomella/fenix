#!/usr/bin/env python3

# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at https://mozilla.org/MPL/2.0/.

# todo: explain script. we do because https://stackoverflow.com/questions/23805915/run-parallel-test-task-using-gradle
# built in doesn't work

from itertools import repeat
import os
from pprint import pprint
import subprocess
import sys
import time

SCRIPT_NAME=os.path.basename(__file__) # todo

ERR_PREFIX = '\n{}: '.format(SCRIPT_NAME)

def assemble_unit_tests():
    print('Assembling unit tests...')
    try:
        subprocess.run(['./gradlew', '--quiet', 'assembleDebugUnitTest'], check=True, stdout=subprocess.DEVNULL)
    except subprocess.CalledProcessError:
        print(ERR_PREFIX + 'failed to assemble unit tests, a pre-requisite step. Exiting...',
            file=sys.stderr)

def bucket_test_paths():
    root_dir = os.getcwd()
    os.chdir('app/src/test/java/org/mozilla/fenix')
    # todo: crash if something added outside of this path?
    # todo: dynamic.

    contents = os.listdir('.')
    files = [path for path in contents if os.path.isfile(path)] # todo: handle files
    dirs = [path for path in contents if os.path.isdir(path)]

    bucket_count = 8
    buckets = []
    for _ in range(bucket_count): buckets.append([])

    for i, dir in enumerate(dirs):
        buckets[i % bucket_count].append('org.mozilla.fenix.{}*'.format(dir))

    os.chdir(root_dir)
    return buckets

def start_tests_in_parallel(buckets):
    print('Running unit tests in parallel with buckets...')
    processes = []
    for bucket in buckets:
        print(); pprint(bucket)
        final_bucket = [element for duple in zip(repeat('--tests'), bucket) for element in duple]

        p = subprocess.Popen(['./gradlew', '--no-daemon', 'testDebug', '--info', '--stacktrace'] + final_bucket, stdout=subprocess.DEVNULL)
        processes.append(p)
    return processes

def wait_and_on_failure_kill_all(processes):
    failed = False
    while True:
        for process in processes:
            returncode = process.poll()
            if returncode == None: continue

            processes.remove(process)
            if returncode == 1:
                failed = True

        if failed: break
        if len(processes) == 0: break
        time.sleep(1)

    if failed:
        for process in processes:
            process.terminate()
        print('test failed') # todo: explain who dun it.
    else:
        print('tests passed')

def main():
    # todo: set base directory for gradle's sake.

    # We're going to run multiple gradle daemons in parallel. To minimize conflict, we
    # assemble the unit tests before so all daemons can re-use the same cached content.
    assemble_unit_tests()

    buckets = bucket_test_paths()
    processes = start_tests_in_parallel(buckets)
    wait_and_on_failure_kill_all(processes)

    # Print duration?
    # todo: kill processes on ctrl-c

if __name__ == '__main__':
    main()
