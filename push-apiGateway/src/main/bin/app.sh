#!/bin/sh
cd `dirname $0`
cd ..
APP_HOME=`pwd`

usage(){
    echo "Usage: ${0##*/} {start|stop|restart} "
    exit 1
}

running(){
  local PID=$(cat "$1" 2>/dev/null) || return 1
  kill -0 "$PID" 2>/dev/null
}

stop(){
  PID=`ps aux | grep java | grep "$APP_HOME" | awk '{print $2}'`
  echo "PID:$PID"
  if [ -n "$PID" ]
  then
    echo "APP is running,PID:$PID"
    kill -9 "$PID" 2>/dev/null
    sleep 1
  fi
}


[ $# -gt 0 ] || usage

stop

echo $APP_HOME
LOG_DIR=$APP_HOME/logs
LIB_DIR=$APP_HOME/lib
CONF_DIR=$APP_HOME/conf

#RESLOVE APP_HOME:
CLASSPATH=$CONF_DIR
for i in "$LIB_DIR"/*.jar; do
   CLASSPATH="$CLASSPATH":"$i"
done

echo $CLASSPATH

#RESLOVE APP_PID:
if [ -z "$APP_PID" ]
then
  APP_PID="$APP_HOME/app.pid"
fi
echo $APP_PID

JAVA_OPTS=" -Dvertx.logger-delegate-factory-class-name=io.vertx.core.logging.SLF4JLogDelegateFactory -Djava.security.egd=file:/dev/./urandom "
JAVA_MEM_OPTS=" -server -Xms128m -Xmx1g -XX:+PrintGCDetails -Xloggc:logs/gc.log -XX:+PrintGCTimeStamps  -XX:SurvivorRatio=2 -XX:+UseParallelGC "


echo -e "Starting ...\c"
nohup java $JAVA_OPTS $JAVA_MEM_OPTS -classpath $CLASSPATH com.vertxjava.blog.App $CONF_DIR/config.json > logs/run.log 2>&1 &
sleep 1
echo "OK!"
PIDS=`ps -f | grep java | grep "$APP_HOME" | awk '{print $2}'`
echo "PID: $PIDS"
exit 0
