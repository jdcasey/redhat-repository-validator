redhat-repository-validator
===========================

[![Build Status](https://travis-ci.org/thradec/redhat-repository-validator.png)](https://travis-ci.org/thradec/redhat-repository-validator)


**redhat-repository-validator** is a tool used to validate the internal consistency of a maven artifact repository. It can also be used for validation of the associated distribution directory.

Released versions can be download from [Bintray](https://bintray.com/thradec/generic/redhat-repository-validator/). 


Building
--------

- prerequisites: Java, Maven and Git
- clone project from github `$ git clone git@github.com:thradec/redhat-repository-validator.git`
- go into the newly created directory `$ cd redhat-repository-validator`
- and run maven build `$ mvn clean package`
- executable distribution is available in `target/redhat-repository-validator-$VERSION` directory or zip file


Usage
-----

This tool can be run from command line via `redhat-repository-validator` script. The only prerequisite is Java 1.6 or greater on path.

Here is help output: 


    redhat-repository-validator is a tool used to validate the internal consistency of a maven artifact repository.
    
    Usage: redhat-repository-validator [-c <file>] [-h] [-lr <dir>] [-rr <url>] [-vr <dir>] [-vd <dir>]
        -c,--config <file>                 use given configuration file,
                                           default value is `redhat-repository-validator-config.xml`
        -h,--help                          print help and exit
        -lr,--local-repository <dir>       use given local repository,
                                           default value is `workspace/local-repository`
        -rr,--remote-repository <url>      use given remote repository, this option can be used multiple times,
                                           default remote repository is only maven central
        -vr,--validated-repository <dir>   validate given repository,
                                           default value is `workspace/validated-repository`
        -vd,--validated-distribution <dir> validate given distribution, verify if current distribution is valid
                                           default value is `workspace/validated-distribution`
    
    Example: 
        to run against a given validated repository directory, use: 
        $ redhat-repository-validator -vr ~/myrepository


Validators
----------

- `DependenciesValidator` try to resolve all required dependencies (scope test, runtime and provided, or optional dependencies are skipped)
- `ModelValidator` make sure that all pom files are "loadable" (maven can load it's model with strict validation level)
- `ChecksumValidator` validate checksums for all repository artifacts (by default  readme and example settings.xml are excluded from this rule)
- `JarSignatureValidator` validate that all jar files are signed/unsigned
- `SuspiciousFileValidator` try to find suspicious files in repository (eg. jar without pom, checksum without source file, empty directory, etc...)
- `BestPracticesValidator` validate rules defined for maven central repository, more details [here](https://docs.sonatype.org/display/Repository/Central+Sync+Requirements)
- `BomDependencyNotFoundValidator` try to resolve all artifacts defined in dependency management
- `BomUnmanagedVersionValidator` try to find artifacts which are not defined in any bom files
- `BomAmbiguousVersionValidator` try to find artifacts which version is defined ambiguous in bom files
- `BomVersionPropertyValidator` try to find boms which define dependencies without version property
- `VersionAmbiguityValidator` try to find artifacts, which have multiple versions in repository
- `VersionOverlapValidator` try to find artifacts, which overlap with others remote repositories
- `VersionPatternValidator` try to find artifacts, which version doesn't match regex pattern (eg. -redhat-x postfix)
- `JarSourcesValidator` try to find artifacts, which do not contain sources within them(verify if _*-sources.jar_ exists)
- `XmlFileValidator` try to find xml files and then verify if they are valid
- `DistributionValidator` try to validate artifacts in distribution against validated repository
- `OsgiVersionValidator` try to find artifacts, which version doesn't match OSGI pattern (by default disabled, via filter configuration)
- `RemoteRepositoryCompareValidator` try to ensure that every artifact in validated repository is available online and is binary same (it has to be explicitly enabled via configuration, it needs remote repository url and comparing strategy)
- `RemoteRepositoryCollisionValidator` try to ensure that artifact is not already published in given remote repository, in default configuration it is used against maven central repository (https://repo1.maven.org/maven2/) and JBoss repository (https://repository.jboss.org/nexus/content/groups/public-jboss/)  


Reporters
---------

- `DefaultReporter` produces simple text reports, which are writen by default into log and into file `workspace/report.txt`
- `SurefireXmlReporter` produces xml files in same format like maven surefire plugin, which can be consumed by tools like Jenkins, default output directory is `workspace/surefire-reports`


How to
------

#### How to change configuration ?

Logging configuration can be changed in `redhat-repository-validator-logback.xml` file, default logger output is console and file log.txt, located in workspace subdirectory.
Tool configuration can be changed in `redhat-repository-validator-config.xml` file and it contains some examples already.


#### How to add whitelist/filter ?

Into each validator is injected file filter (interface `IOFileFilter`), which allows to skip selected files. 
By conventions, filter beans have id like validator name with suffix filter, for example `checksumValidatorFilter`. 
These filter beans can be redefined in external configuration file, see examples in xml or groovy `fooValidatorFilter `.


#### How to add remote repository ?

Remote repository can be added via command line options `-rr`, for example `$ redhat-repository-validator -rr file://foo-repository`. 
Or permanently added in configuration file, see `fooRepository` snippet, where is variant with user authentication.


#### How to add custom validation/report ?

Validators/reporters have to implement interface `com.redhat.repository.validator.Validator/Reporter`, 
for simple example take a look at `ChecksumValidator` implementation. 
Jar file with the new validator/reporter needs to be added into `lib` subdirectory, so it will automatically end up 
on classpath.
As a last step new bean has to be configured in `redhat-repository-validator-config.xml`, see an example with `fooValidator`.        


#### How to execute only specified validators ?

By default all validators on classpath are gathered and executed. There might be cases where running all of them is not practical.
The class that executes the validators is an ordinary bean. That means it can be redefined in the `redhat-repository-validator-config.xml`.
Using the example below will result in execution of only `JarSourcesValidator` and `JarSignatureValidator`.

```xml
...
<bean id="validationExecutor" class="com.redhat.repository.validator.ValidationExecutor">
    <constructor-arg>
        <list>
            <ref bean="jarSourcesValidator"/>
            <ref bean="jarSignatureValidator"/>
        </list>
    </constructor-arg>
</bean>
...
```


#### How to execute only specified reporters ?

Similarly as with validators, there might be cases where running all of reporters is not practical.
The class that executes the reporters is also an ordinary bean and can be redefined in the `redhat-repository-validator-config.xml`.
Using the example below will result in execution of only the `SurefireXmlReporter`.

```xml
...
<bean id="reportingExecutor" class="com.redhat.repository.validator.ReportingExecutor">
    <constructor-arg>
        <list>
            <ref bean="surefireXmlReporter"/>
        </list>
    </constructor-arg>
</bean>
...
```


#### How to filter out (ignore) specified exceptions ?

redhat-repository-validator allows filtering (ignoring) of the exceptions using one of the following configuration options. The
configuration is XML based and needs to be added into the `redhat-repository-validator-config.xml`. Such filtered exceptions will not
be passed to the reporters.

##### Filtering exceptions based on filename and filepath regex
Following example shows filtering of the `SuspiciousFileException` for all files ending with `.xsd`:

```xml
...
<filter:file name-regex=".*\.xsd" exception="SuspiciousFileException" />
...
```

Following example shows filtering of the `SuspiciousFileException` with `foobar` in message for all files ending with `.xsd` and with `/foo/bar/` in relative path:

```xml
...
<filter:file path-regex=".*/foo/bar/.*\.xsd" exception="SuspiciousFileException" exception-msg-regex=".*foobar.*"/>
...
```

Please consult the sample `redhat-repository-validator-config.xml` for more examples of the filename based filters.

##### Filtering (bom) dependency not found exceptions
`DependencyNotFoundException`s and `BomDependencyNotFoundException`s can be filterd out based on the information about
the missing artifact and the validated artifact (e.g. the pom file that references the missing artifact).
The artifact regular expressions have format `groupId:artifactId:extension:[classifier]:version` and use the standard Java regular
expression syntax.
Following example shows filtering of the `DependencyNotFoundExcepiton` for the specified missing artifact referenced
from the artifact matching the validated-artifact regex.
```xml
...
<filter:dependency-not-found missing-artifact="com.acme:finance:.*:jar" validated-artifact="com.acme:parent:.*:pom" />
...
```
Please consult the sample `redhat-repository-validator-config.xml` for more examples of the `(bom-)dependency-not-found` filters.


#### How to add custom RemoteRepositoryCollisionValidator

If you want to check that artifact is not already published in some other remote repository, 
for example RedHat techpreview repository (https://maven.repository.redhat.com/techpreview), 
you have to add folowing configuration into `redhat-repository-validator-config.xml`.

```xml
<bean id="redhatTechpreviewCollisionValidator" class="com.redhat.repository.validator.impl.remoterepository.RemoteRepositoryCollisionValidator">
    <constructor-arg name="remoteRepositoryUrl" value="https://maven.repository.redhat.com/techpreview" />
    <constructor-arg name="checksumProvider" ref="nexusChecksumProvider" />
</bean>
<bean id="nexusChecksumProvider" class="com.redhat.repository.validator.impl.remoterepository.ChecksumProviderNexus" />    
```

#### How to add custom BOM filter

There is no way how to reliably recognize BOMs. 
The default implementation of `BomFilter` check if the artifact name or group contains "bom" string. 
If it is not sufficient, you can provide your own implementation, or you can provide some hint in form of regular expression, which is use against GAV, see example bellow.

```xml
<bean id="bomFilter" class="com.redhat.repository.validator.impl.bom.BomFilterSimple">
    <constructor-arg name="bomGavRegex" value=".*:myBillOfMaterials:pom:.*" />
</bean>
``` 



