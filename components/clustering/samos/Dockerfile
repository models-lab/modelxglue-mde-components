FROM continuumio/miniconda3

WORKDIR /

RUN mkdir -p /scripts/SCICO-D-21-00209

COPY environment.yml /scripts/

RUN conda env create -f /scripts/environment.yml -n r_env

COPY SCICO-D-21-00209 /scripts/SCICO-D-21-00209

SHELL ["conda", "run", "-n", "r_env", "/bin/bash", "-c"]
RUN R < /scripts/SCICO-D-21-00209/packages.R --no-save

RUN conda install -c conda-forge maven -y

RUN cd /scripts/SCICO-D-21-00209; \
    mvn package -Dmaven.test.skip=true

RUN echo "install.packages('/scripts/SCICO-D-21-00209/lib/vegan_2.4-3.tar.gz', repos = NULL)" > req.R
RUN R < req.R --no-save

ENV R_HOME=/opt/conda/envs/r_env/lib/R
ENV JRI_PATH=/opt/conda/envs/r_env/lib/R/library/rJava/jri





