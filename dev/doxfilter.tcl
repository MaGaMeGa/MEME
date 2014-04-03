# Removes annotations from java files, because doxygen misinterprets them
#
set fn [lindex $argv 0]
if {![file exists $fn]} {
   set fn [string trimleft $fn " "]
}
set f [open $fn]
set text [read $f]
close $f
if {[string equal -nocase ".java" [file extension $fn]]} {

   # Annotations are removed if they have the form @name or @name(...)
   # and are the first in a line
   #
   regsub -all -line {^(\s*)@\w+(\([^)]*\))?} $text {\1} text2

} else {
   set text2 $text
}
puts $text2
