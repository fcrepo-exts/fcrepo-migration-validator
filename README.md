# fcrepo-migration-validator

![Build Status](https://github.com/fcrepo-exts/fcrepo-migration-validator/workflows/Java%20CI%20with%20Maven/badge.svg)

A command-line tool for validating migrations of Fedora 3 datasets to Fedora 6.

## Usage

General usage is as follows:
```bash
java -jar target/fcrepo-migration-validator-0.1.0-SNAPSHOT-driver.jar [cli options | --help],
```

The following CLI options for validations are available:
```
Usage: fcrepo-migration-validator [-ChV] [--debug] [-a=<algorithm>]
                                  [-c=<ocflRootDirectory>]   
                                  [-d=<f3DatastreamsDir>] [-e=<f3ExportedDir>]
                                  [-f=<f3hostname>] [-i=<indexDir>]
                                  [-o=<f3ObjectsDir>] [-p=<objectsToValidate>]
                                  [-r=<resultsDirectory>] [-R=<reportType>]
                                  -s=<f3SourceType> [-t=<threadCount>]
  -h, --help       Show this help message and exit.
  -V, --version    Print version information and exit.
  -s, --source-type=<f3SourceType>
                   Fedora 3 source type. Choices: akubra | legacy | exported
  -d, --datastreams-dir=<f3DatastreamsDir>
                   Directory containing Fedora 3 datastreams (used with
                     --source-type 'akubra' or 'legacy')
  -o, --objects-dir=<f3ObjectsDir>
                   Directory containing Fedora 3 objects (used with
                     --source-type 'akubra' or 'legacy')
  -e, --exported-dir=<f3ExportedDir>
                   Directory containing Fedora 3 export (used with
                     --source-type 'exported')
  -f, --f3hostname=<f3hostname>
                   Hostname of Fedora 3, used for replacing placeholder in 'E'
                     and 'R' datastream URLs
                     Default: fedora.info
  -i, --index-dir=<indexDir>
                   Directory where cached index of datastreams (will reuse
                     index if already exists)
  -r, --results-dir=<resultsDirectory>
                   Directory where validation results are placed
  -c, --ocfl-root-dir=<ocflRootDirectory>
                   The root directory of the Fedora OCFL.
  -t, --threads=<threadCount>
                   The number of threads for parallel processing. Default 5
  -p, --pid-file=<objectsToValidate>
                   PID file listing which Fedora 3 objects to validate
  -R, --report-type=<reportType>
                   Type of report to generate: html, csv, tsv
                     Default: html
  -C, --checksum   Enable checksum validations of datastreams
  -a, --algorithm=<algorithm>
                   The digest algorithm to use during checksum validation:
                     sha256, sha512
                     Default: sha512
  --debug      Enables debug logging
```

## Development

The migration validator is built with [Maven 3](https://maven.apache.org) and requires Java 11 and Maven 3.1+

To build the migration validator and run integration tests, run:
```bash
mvn clean verify
```
