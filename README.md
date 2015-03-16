[JSR-88](http://jcp.org/en/jsr/detail?id=88) implementation for ssh deployment.

Implementation based on [JSch](http://www.jcraft.com/jsch/)

Implementation note
===================

 * Only ModuleType.EJB is supported. Or file with name `*`.jar. All other module types or extensions will cause !IllegalArgumentException
 * File or InputSream must be executable jar [archive](http://java.sun.com/j2se/1.3/docs/guide/jar/jar.html#Main%20Attributes)
 * Deploy plan is a standart java.util.Properties file or stream. Valid properties:
 
    * JAVA_OPTS - will be added as jvm options at startup script. no need to define "-jar". It will be automatically added
    * JAR_OPTS - will be added as program options.
 
 * J2EE-!DeploymentFactory-Implementation-Class is ru.onlytime.ssh.deploy.!DeploymentFactoryImpl
 * connection URL: ```deployer:ru.onlytime.ssh:<host>:<port>:<path>```
 * redeploy is supported
 * locale is not supported
 * DConfigBeanVersion is not supported
