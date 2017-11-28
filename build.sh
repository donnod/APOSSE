# update submodules
git submodule update --init --recursive

# build JavaReedSolomon using maven
cp helper/JavaReedSolomon_pom.xml JavaReedSolomon/pom.xml
cd JavaReedSolomon
mvn install

# build apo-sse
cd ..
mvn install

# build APO based sse test in Clusion
cp helper/TestLocalRR2LevAPO.java Clusion/src/test/java/org/crypto/sse/
cd Clusion
if [ ! -f pom.xml.bak ]; then
    mv pom.xml pom.xml.bak
fi
# patch pom.xml file of Clusion to include apo-sse module
sed -e '/<dependencies>/{r ../helper/Clusion_pom_patch.xml' -e 'd}' pom.xml.bak > pom.xml
mvn install
