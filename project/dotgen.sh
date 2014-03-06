#!/bin/bash
echo Rendering CFG and DOM 
dot -Tpng $1.cfg.dot -o $1.cfg.png
dot -Tpng $1.dom.dot -o $1.dom.png
dot -Tpng $1.ig.dot -o $1.ig.png