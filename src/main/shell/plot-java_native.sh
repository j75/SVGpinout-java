#!/usr/bin/env sh

if ! command -v gnuplot >/dev/null 2>&1 ; then
        echo "gnuplot is required -> install gnuplot package please!"
        exit 1
fi

DEBUG=0

# OPTIMIZE=1 src/main/shell/plot-java_native.sh 1500 
# journalctl -t $LOGNAME -f
OPTIMIZE=${OPTIMIZE:-0}

SCRIPT_NAME=$(basename "$(realpath "$0")" )
WORKDIR=$(mktemp --tmpdir -d "${SCRIPT_NAME}"-XXXX)

trap 'rm -rf $WORKDIR' EXIT HUP TERM

_dat_file="$WORKDIR/java_native.dat"
_plot_file="$WORKDIR/java_native.plt"

TESTFILE='src/test/resources/MMP1206.csv'
LOGLEV='-Dorg.slf4j.simpleLogger.defaultLogLevel=warn'

msg()
{
  if [ $DEBUG -gt 0 ]; then
    echo "$@"
  fi
}

usage()
{
    echo "$0 <nb. of tries>"
}

fill_data()
{
  if [ ! -r target/svgpinout.jar ]; then
    echo "No jar file - please run 'mvn package -DskipTests'!"
    exit 3
  fi
  if [ ! -r target/svgpinout ]; then
    echo "No native executable - please run 'mvn package -Pnative -DskipTests'!"
    exit 4
  fi
  msg "running $1 times"
  if [ "$1" -le 20 ]; then
        incr=1
  elif [ "$1" -le 100 ]; then
        incr=2
  else
        incr=5
  fi

  for i in $(seq 1 $incr "$1"); do
        msg "# of iterations: $i"
        logger -t "$LOGNAME" -p user.info "OPTIMIZE = $OPTIMIZE, $iteration # $i"
        if [ $OPTIMIZE -gt 0 ]; then
            # Java
          _javadur=$(java "$LOGLEV" -jar target/svgpinout.jar -s -c -x fut -r "$i" -o /tmp "$TESTFILE" | grep msec | awk '{print $4/1000}')
          # Native
          _natdur=$(target/svgpinout "$LOGLEV" -s -x fut -c -r "$i" -o /tmp "$TESTFILE" | grep msec | awk '{print $4/1000}')
        else
          # Java
          _javadur=$(java "$LOGLEV" -jar target/svgpinout.jar -s -r "$i" -o /tmp "$TESTFILE" | grep msec | awk '{print $4/1000}')
          # Native
          _natdur=$(target/svgpinout "$LOGLEV" -s -r "$i" -o /tmp "$TESTFILE" | grep msec | awk '{print $4/1000}')
        fi
        printf "%d %f %f\n" "$i" "$_javadur" "$_natdur" >> "$_dat_file"
  done
}

plot_data()
{
    if [ ! -r "$_dat_file" ]; then
        echo "Cannot read data file $_dat_file!"
        exit 2
    fi
    cat << EOF > "$_plot_file"
set term png small size 800,600
set output "$1"

set ylabel "Time elapsed (seconds)"
set xlabel "Nb. of executions"

set yrange [0:*]

plot "$_dat_file" using 1:2 title 'Java' with lines, "$_dat_file" using 1:3 title 'Native' with lines
EOF
}

main ()
{
    _nbexecs=10
    if [ $# -gt 0 ]; then
        _nbexecs=$1
    fi
    if [ "$_nbexecs" -lt 2 ]; then
        _nbexecs=2
    fi

    fill_data "$_nbexecs"
    plot_data "/tmp/java_native-$_nbexecs.png"
    gnuplot "$_plot_file"

    echo "Graph created: /tmp/java_native-$_nbexecs.png"
}

main "$@"
