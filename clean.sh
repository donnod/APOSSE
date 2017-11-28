# clean Clusion
cd Clusion
mvn clean
rm src/test/java/org/crypto/sse/TestLocalRR2LevAPO.java
if [ -f pom.xml.bak ]; then
    mv pom.xml.bak pom.xml
fi

# clean apo-sse
cd ..
mvn clean

# clean JavaReedSolomon
cd JavaReedSolomon
if [ -f pom.xml ]; then
    mvn clean
    rm pom.xml
fi
