#!/bin/bash

# Get the source code of SAMOS
pushd .
cd components/clustering/samos/
git clone https://github.com/Antolin1/SCICO-D-21-00209.git
popd

# Get Ecorebert models
pushd .
cd components/recommendation/ecorebert
./download_model.sh
popd
