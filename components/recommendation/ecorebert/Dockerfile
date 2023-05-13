FROM devtography/python-cuda

WORKDIR /


RUN mkdir -p /scripts
RUN mkdir -p /model


COPY models /model/models
COPY tokenizers /model/tokenizers
COPY inference.py /scripts
COPY requirements.txt /

RUN pip install -r requirements.txt

