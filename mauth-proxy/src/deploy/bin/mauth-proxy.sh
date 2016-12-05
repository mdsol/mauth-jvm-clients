#!/usr/bin/env bash

BIN=`dirname "$0"`
BASE=$BIN/..
java -cp $BASE/config:$BASE/lib/* com.mdsol.mauth.proxy.ProxyServer
