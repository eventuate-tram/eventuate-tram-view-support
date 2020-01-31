#! /bin/bash

set -e

export DATABASE=mysql
export MODE=polling

./_build-and-test-all.sh
