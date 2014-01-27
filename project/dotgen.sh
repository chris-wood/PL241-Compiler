 #!/bin/bash
 echo Enter file prefix
 read INPUT
 echo Running: dot -Tps $INPUT.dot -o $INPUT.ps
 dot -Tps $INPUT.dot -o $INPUT.ps