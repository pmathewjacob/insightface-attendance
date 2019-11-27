# Light weight attendance check 

## Introduction

This is a project based on [InsightFace: 2D and 3D Face Analysis Project](https://github.com/deepinsight/insightface). 
### Feature
- Cut out face images from pictures
- Edit and save face embedded features
- Check attendance

## How to install

- Clone the repository
```console
git clone https://github.com/nv-quan/insightface-attendance.git
cd insightface-attendance
```
- Install and open python virtual environment (optional) 
```console
python3 -m venv ./venv
source ./venv/bin/activate
```
- Install requirements
```console
pip install -r requirements.txt
```

## Usage

```console
cd src/api/
```
Before running the first time, create a new folder named faces
```console
mkdir faces
```
1. To add faces 
```console
python demo.py -a --source-image ./source.png
```
2. To edit faces data
```console
python demo.py -e
```
3. To check attendance
```console
python demo.py -c --source-image ./source.png
```

We provide model of VarGFaceNet trained on cleaned MS1M dataset with accuracy:

| Method  | LFW(%)  | CFP-FP(%) | AgeDB-30(%) | 
| ------- | ------- | --------- | ----------- | 
|  Ours   | 0.997   | 0.971     | 0.968       |
