#!/usr/bin/env bash
#
set -e

DEBUG=0

OPTIMIZE=0

SCRIPT_NAME=$(basename "$(realpath "$0")" )
SVGPBM_TDIR=$(mktemp --tmpdir -d "${SCRIPT_NAME}"-XXXX)
PKGS_FILE='src/main/resources/packages.csv'
readonly PIN_TYPES=("A" "C" "D" "G" "I" "K" "M" "O")
readonly LOGOS=("motorola" "nec" "sanyo" "snk" "ti" "yamaha" "zilog")
NBCHIPS=100
readonly PERF_REPEAT_NB=1

trap 'rm -rf $SVGPBM_TDIR' EXIT HUP TERM

msg()
{
  if [ $DEBUG -gt 0 ]; then
    echo "$@"
  fi
}

usage()
{
    echo "$0 [-v | -h | -p | -t] <nb. of chip files>"
    echo "  where"
    echo "    -v => verbose"
    echo "    -t => create test tarball of csv files"
    echo "    -o => run Java optimisations"
    echo "    -h => help (this message)"

}

# create_csv_files <nb. chips>
create_csv_files()
{
  local _nbchips=$1
  msg "#of fake chips = $_nbchips"
  chips_arr=()
  nb_pins=()
  while read -r line; do
    # Process $line here
    first_field=$(echo "$line" | cut -d',' -f1)
    if [ "$first_field" = 'name' ]; then
      continue;
    fi
    chips_arr+=("$first_field")
    second_field=$(echo "$line" | cut -d',' -f2)
    nb_pins+=("$second_field")
  done < "$PKGS_FILE"
  msg "Number of chip cases: ${#chips_arr[@]}"
  msg
  msg "Chips cases:"
  msg " ${chips_arr[*]}"
  msg "  # of pins:"
  msg " ${nb_pins[*]}"

  _nblogos=${#LOGOS[@]}
  msg "Nb. of logos = $_nblogos"
  for i in $(seq "$_nbchips"); do
    _chip_file="${SVGPBM_TDIR}"/csv/chip-"$i".csv
    _idx_chip=$((i-1))
    _idx_chip=$((_idx_chip%${#chips_arr[@]}))
    _chip_case=${chips_arr[$_idx_chip]}
    _nb_pins=${nb_pins[$_idx_chip]}
    _idx_logo=$((_idx_chip%_nblogos))
    _logo=${LOGOS[$_idx_logo]}
    echo "fake-${i},fake-${i}_pinout,${_chip_case},logo_${_logo}.png" > "$_chip_file"
    echo "VCC,P" >> "$_chip_file"
    echo "GND,P" >> "$_chip_file"
    #msg "index of chip = $_idx_chip case = $_chip_case nb. pins = $_nb_pins"
    for j in $(seq 3 "$_nb_pins"); do
      _idx_pt=$((j%${#PIN_TYPES[@]}))
      if [ $((j%3)) == 0 ]; then
        _dir="IN"
      elif [ $((j%3)) == 1 ]; then
        _dir="OUT"
      else
        _dir="BIDIR"
      fi
      echo "PIN$j,${PIN_TYPES[$_idx_pt]},$_dir" >> "$_chip_file"
    done
  done
}

create_test_files()
{
    msg "Temporary folder: $SVGPBM_TDIR"

    mkdir -p "${SVGPBM_TDIR}"/{csv,svg}
    mkdir -p "${SVGPBM_TDIR}"/"$(dirname $PKGS_FILE)"
    install -m 0644 $PKGS_FILE "${SVGPBM_TDIR}"/"$(dirname $PKGS_FILE)"
    install -d "$(dirname $PKGS_FILE)"/logos "${SVGPBM_TDIR}"/"$(dirname $PKGS_FILE)"

    if [ $# -gt 0 ]; then
        NBCHIPS=$1
    fi

    NBPCK=$(wc -l "$PKGS_FILE" | awk '{print $1}')
    readonly NBPCK
    if [ "$NBCHIPS" -lt $((NBPCK - 1)) ]; then
        NBCHIPS=$((NBPCK - 1))
        msg "Number of chips augmented to $NBCHIPS"
    fi
    msg "Nb. of fake chip files = $NBCHIPS"

    create_csv_files "$NBCHIPS"
}

benchmark_python()
{
    local _curdir
    _curdir=$(pwd)
    pushd "${SVGPBM_TDIR}" > /dev/null
    echo "Results of the Python script execution"
    #time \
    sudo chrt -f 99 perf stat -r "$PERF_REPEAT_NB" -d \
        "${_curdir}"/src/main/python/svgpinout.py all > /dev/null
    rm -rf svg/*
    echo "---------------"
    popd > /dev/null
}

benchmark_jar()
{
  if [ $OPTIMIZE -gt 0 ]; then
    echo "Results of the Java jar execution (cache + CompletableFuture)"
    sudo chrt -f 99 perf stat -r "$PERF_REPEAT_NB" -d \
          "${JAVA_HOME}"/bin/java -Dorg.slf4j.simpleLogger.defaultLogLevel=warn \
          -jar ./target/svgpinout.jar -c -o "${SVGPBM_TDIR}"/svg -x fut "${SVGPBM_TDIR}"/csv
  else
    echo "Results of the Java jar execution"
    sudo chrt -f 99 perf stat -r "$PERF_REPEAT_NB" -d \
      "${JAVA_HOME}"/bin/java -Dorg.slf4j.simpleLogger.defaultLogLevel=warn \
      -jar ./target/svgpinout.jar -o "${SVGPBM_TDIR}"/svg "${SVGPBM_TDIR}"/csv
  fi
  echo "---------------"
  rm -rf svg/*
}

benchmark_bin()
{
  if [ $OPTIMIZE -gt 0 ]; then
    echo "Results of the native executable (cache + CompletableFuture)"
    sudo chrt -f 99 perf stat -r "$PERF_REPEAT_NB" -d \
        ./target/svgpinout -Dorg.slf4j.simpleLogger.defaultLogLevel=warn -n \
        -o "${SVGPBM_TDIR}"/svg -c -x fut "${SVGPBM_TDIR}"/csv
  else
    echo "Results of the native executable"
    #time \
    sudo chrt -f 99 perf stat -r "$PERF_REPEAT_NB" -d \
      ./target/svgpinout -Dorg.slf4j.simpleLogger.defaultLogLevel=warn -n \
      -o "${SVGPBM_TDIR}"/svg "${SVGPBM_TDIR}"/csv
  fi
  echo "---------------"
}

build_tarball()
{
  msg "Temporary folder: $SVGPBM_TDIR"

  mkdir -p "${SVGPBM_TDIR}"/csv
  local NBCHIPS=100
  if [ $# -gt 0 ]; then
    NBCHIPS=$1
  fi

  NBPCK=$(wc -l "$PKGS_FILE" | awk '{print $1}')
  readonly NBPCK
  if [ "$NBCHIPS" -lt $((NBPCK - 1)) ]; then
    NBCHIPS=$((NBPCK - 1))
    msg "Number of chips augmented to $NBCHIPS"
  fi
  msg "Nb. of fake chip files = $NBCHIPS"
  if [ -f "./test_csv_${NBCHIPS}.tgz" ]; then
    echo "File test_csv_${NBCHIPS}.tgz exists!"
    exit 2
  fi
  create_csv_files "$NBCHIPS"
  local mydir
  mydir=$(pwd)
  pushd "${SVGPBM_TDIR}" > /dev/null
  if [ $DEBUG -gt 0 ]; then
    tar cvzf "${mydir}"/test_csv_"${NBCHIPS}".tgz csv
  else
    tar czf "${mydir}"/test_csv_"${NBCHIPS}".tgz csv
  fi
  popd > /dev/null
  exit 0
}

main () {
  local create_tarball='n'

  while getopts "htvo" arg; do
    case $arg in
      t) create_tarball='y' ;;
      v) DEBUG=1 ;;
      o) OPTIMIZE=1 ;;
      h) usage; exit 0 ;;
      *) usage; exit 1 ;;
    esac
  done
    # Remove parsed options
  shift $((OPTIND - 1))
  if [ "$create_tarball" = 'y' ]; then
    build_tarball "$*"
  fi

  if [ ! -x target/svgpinout ]; then
    mvn -U clean package -P native -DskipTests
  fi

  create_test_files "$@"

  benchmark_python
  benchmark_jar
  benchmark_bin

  #tree -a "$SVGPBM_TDIR"
  msg "Files were created in $SVGPBM_TDIR folder"
  echo "Benchmarked $NBCHIPS SVG files"
}

main "$@"
