 #!/bin/bash
 echo Enter file prefix
 read INPUT
 echo Running: dot -Tpng $INPUT.dot -o $INPUT.png
 dot -Tpng $INPUT.dot -o $INPUT.png