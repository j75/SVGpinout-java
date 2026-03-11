#!/usr/bin/env sh

if ! command -v gnuplot >/dev/null 2>&1 ; then
        echo "gnuplot is required -> install gnuplot package please!"
        exit 1
fi

# grep '^|' README.md  | tr '|' ' ' > py_java_native.dat

_dat_file='py_java_native.dat'

if [ ! -r "$_dat_file" ]; then
    echo "Cannot read '$_dat_file' file"
    exit 2
fi

_png_file='/tmp/python_java_native.png'

cat << EOF > /tmp/python_java_native-"$$".plt
set term png small size 800,600
set output "$_png_file"

set ylabel "Time elapsed (seconds)"
set xlabel "Nb. of chip files"

set yrange [0:*]

plot "$_dat_file" using 1:2 title 'Python' with lines,\
     "$_dat_file" using 1:3 title 'Java' with lines,\
     "$_dat_file" using 1:4 title 'Native' with lines
EOF

gnuplot /tmp/python_java_native-"$$".plt

echo "Graph created: $_png_file"
rm -f /tmp/python_java_native-"$$".plt
