# NIO File Transport

This project is about to create an event based (non-polling) file transport. In a polling file transport the files are being detected by polling a directory or set of directories using a given time of interval in milliseconds or seconds. Even though there are no files in those directories this scans for the files periodically. This is an overhead and inefficient use of system resources. So, the event based file transport acts as a non-polling transport which will trigger an event if and only if there is a file or a directory is created.

The JDK 7 provided a special package called java.nio. Java NIO (New IO) is an alternative IO API for Java (from Java 1.4), meaning alternative to the standard Java IO and Java Networking API’s. Java NIO offers a different way of working with IO than the standard IO API’s. This package provided a package called `java.nio.file` and it has a file change notification API, called the `WatchService API`. This API enables to register a directory (or directories) with the watch service. When registering, we tell the service which types of events we are interested in: file creation, file deletion, or file modification. When the service detects an event of interest, it is forwarded to the registered process. The registered process has a thread (or a pool of threads) dedicated to watching for any events it has registered for. When an event comes in, it is handled as needed.

### Use case

- https://developer.adroitlogic.com/connectors/docs/17.07/nio_file/nio_file_ingress_connector.html

### More info

- Medium article: https://medium.com/@ldclakmal/nio-file-transport-c0811cb0369b
- Dzone article: https://dzone.com/articles/event-driven-architecture-over-polling-architecture
