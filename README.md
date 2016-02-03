# CreateNewType

Create new type in cTAKES

Make modification to the following files:

1. Create a variable for the new type

    In /ctakes/ctakes-dictionary-lookup-fast/src/main/java/org/apache/ctakes/dictionary/lookup2/util/SemanticUtil.java
    
    Under SemanticUtil, create a variable for the type and define the associated TUIs 
    
      Example 
      
        static private final String[] GENE = { "T028","T085","T086","T087","T088" };  // TUIs related to that type
        
        Create a new set, which uses a hash table, for the newly defined type 
        
        static private final Collection<String> GENE_TUIS = new HashSet<>( Arrays.asList( GENE ) );
    
    Associate the ctakes group id with their TUIs,
    
    Under getTuiSemanticGroupId, add a case to the "if-else" block
    
    	Example
    
        else if ( GENE_TUIS.contains( tui ) ) { //**
           return CONST.NE_TYPE_ID_GENE;  // or an integer, e.g. 11
        }


2. Add new cases in the switch block inside the term consumer so that it will identify the annotated terms

    In /ctakes/ctakes-dictionary-lookup-fast/src/main/java/org/apache/ctakes/dictionary/lookup2/consumer/DefaultTermConsumer.java
    
    In createSemanticAnnotation, add a new case
    
    	Example
    
          case NE_TYPE_ID_GENE: {   //defined in CONST.java 
                 return new GeneMention( jcas );
          }


3. Constants are defined in 
    /ctakes/ctakes-type-system/src/main/java/org/apache/ctakes/typesystem/type/constants/CONST.java


4. Add new type to cTAKES’ type system, 

    In /ctakes/ctakes-type-system/src/main/resources/org/apache/ctakes/typesystem/types/TypeSystem.xml
    Define the new type within the following
    
      Example in TypeSystem.xml
          <typeSystemDescription xmlns="http://uima.apache.org/resourceSpecifier">
            <types>
          ….
          ….
            <typeDescription>
            <name>org.apache.ctakes.typesystem.type.textsem.GeneMention</name>
            <description>Corresponds to the UMLS Gene or Genome semantic group</description>
            <supertypeName>org.apache.ctakes.typesystem.type.textsem.EntityMention</supertypeName>
            <features>
            <featureDescription>
            <name>entity</name>
            <description/>
            <rangeTypeName>org.apache.ctakes.typesystem.type.refsem.Entity</rangeTypeName>
            </featureDescription>
            </features>
            </typeDescription>
          ….
          ….
            </types>
          </typeSystemDescription>



5. Once all the changes are made, generate two new jar files from these directories (below) to put in {binary-ctakes-directory}/lib/

    •	/ctakes/ctakes-dictionary-lookup-fast/
    
    •	/ctakes/ctakes-type-system/
    
    Modify the pom files of their respective directories to use the new jar files
    
    Changed the version name for common type system  in /ctakes/ctakes-type-system/pom.xml:  
    
      Example
              <name>Apache cTAKES common type system</name>
              <description>Defines the cTAKES type system UIMA annotation classes.</description>
              <version>3.2.3-SNAPSHOT-selina</version>
      
    Changed dependency name for ctakes-type-system in /ctakes/ctakes-dictionary-lookup-fast/pom.xml: 
    
      	Example
               <dependency>
                      <groupId>org.apache.ctakes</groupId>
                      <artifactId>ctakes-type-system</artifactId>
                      <version>3.2.3-SNAPSHOT-selina</version>
               </dependency>


6. Generate two new jar files from these directories to put in binary-ctakes-directory/lib/

    $ cd /ctakes/ctakes-dictionary-lookup-fast/

    $ mvn package

    $ cd target

    $ cp ctakes-dictionary-lookup-fast-3.2.3-SNAPSHOT.jar  {binary-ctakes-directory}/lib/.


    
    $ cd /ctakes/ctakes-type-system/

    $ mvn package

    $ cd target

    $ cp ctakes-type-system-3.2.3-SNAPSHOT-selina.jar {binary-ctakes-directory}/lib/.



