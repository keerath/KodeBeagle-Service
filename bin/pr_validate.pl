#!/usr/bin/perl

# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
#
#    http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

my @lines = split /\n/, `git log --no-decorate --no-merges --pretty=format:%s origin...HEAD`;

foreach (@lines) {
    if(!/#\d+\s[\w\s]+.*/) {
        print STDERR "The commit \n $_ \n Does not follow the commit message convention.\n";
        print STDERR "Please see other commit messages for examples:\n";
        print STDERR `git log --no-decorate --no-merges --pretty=format:%s 13de794...b62a9cd`;
        print STDERR "\n\n";
        exit(12);
    }
}

print "SUCCESS: PR validations.\n";
