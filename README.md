# APOSSE

APOSSE is a Java Package for Access Pattern Obfuscation for Searchable Symmetric
Encryption. It is an prototype implementation of the following paper:

[INFOCOM18] *Differentially Private Access Patterns for Searchable Symmetric Encryption* by G. Chen and T.H. Lai and M. Reiter and Y. Zhang

## Implementation

APOSSE currently supports Clusion, specifically the static single-keyword SSE scheme, 2Lev. Since Clusion does not provide any API to access the indexing lookup table. The example code of 2Lev with access-pattern obfuscation need to be included into the Clusion package to be built and tested.

## Build Instructions

    `cd APOSSE`
	
	`./build.sh`

## Quick Test

    `export CLASSPATH=$CLASSPATH:/home/xxx/APOSSE/Clusion/target/Clusion-1.0-SNAPSHOT-jar-with-dependencies.jar:/home/xxx/APOSSE/Clusion/target/test-classes`
    
    `java org.crypto.sse.TestLocalRR2LevAPO`	
	
