.PHONY: jar install

#JAVAC = javac ${JAVACFLAGS}
JAVAC = javac

all: 
	javac -cp libraries/coremidi4j-1.6.jar:edisyn $$(find edisyn -name '*.java') 

run:
	java -cp libraries/coremidi4j-1.6.jar:. edisyn.Edisyn

indent:
	touch ${HOME}/.emacs
	find . -name "*.java" -print -exec emacs --batch --load ~/.emacs --eval='(progn (find-file "{}") (mark-whole-buffer) (setq indent-tabs-mode nil) (untabify (point-min) (point-max)) (indent-region (point-min) (point-max) nil) (save-buffer))' \;

jar:
	rm -rf install/edisyn.jar uk META-INF
	${JAVAC} edisyn/*.java edisyn/*/*.java edisyn/*/*/*.java 
	touch /tmp/manifest.add
	rm /tmp/manifest.add
	echo "Main-Class: edisyn.Edisyn" > /tmp/manifest.add
	cd libraries ; jar -xvf coremidi4j-1.6.jar
	mv libraries/META-INF . ; mv libraries/uk .
	jar -cvfm install/edisyn.jar /tmp/manifest.add edisyn/synth/Synths.txt `find edisyn -name "*.class"` `find edisyn -name "*.init"` `find edisyn -name "*.html"` `find edisyn -name "*.png"` `find edisyn -name "*.jpg"` `find edisyn -name "*.txt"` uk/ META-INF/
	echo jar -cvfm install/edisyn.jar /tmp/manifest.add edisyn/synth/Synths.txt `find edisyn -name "*.class"` `find edisyn -name "*.init"` `find edisyn -name "*.html"` `find edisyn -name "*.png"` `find edisyn -name "*.jpg"` `find edisyn -name "*.txt"` uk/ META-INF/
	rm -rf uk META-INF

install: jar
	rm -rf install/Edisyn.app install/bundles install/Edisyn.dmg.html install/Edisyn.dmg.jnlp
	- javapackager -deploy -native dmg -srcfiles install/edisyn.jar -appclass edisyn.Edisyn -name Edisyn -outdir install -outfile Edisyn.dmg -v
	- mv install/bundles/Edisyn-1.0.dmg install/Edisyn.dmg
	rm -rf install/bundles install/Edisyn.dmg.html install/Edisyn.dmg.jnlp 

