import face_model
import cv2
import argparse
import numpy as np
import json

parser = argparse.ArgumentParser(description='do verification')

parser.add_argument('--image-size', default='112,112', help='')
parser.add_argument('--model', default='../../models/model,1', help='path to load model.')
parser.add_argument('--gpu', default=0, type=int, help='gpu id')
parser.add_argument('--threshold', default=1.24, type=float, help='ver dist threshold')
parser.add_argument('--source-image', default='./source.png', help='path to source image.')
parser.add_argument('--source-data', default='./source.json', help='path to source data.')
args = parser.parse_args()


source_img = cv2.imread(args.source_image)
print(source_img.shape)

if source_img is None:
    print('Source image is invalid')
else:
    model = face_model.FaceModel(args)
    faces = model.get_all_faces(source_img)
    print('Detected %d faces' % (len(faces)))
    try:
        with open(args.source_data) as data_file:
            data = json.load(data_file)
            i = data['num_pics']
    except IOError:
        i = 0
        data = dict()
    for face in faces:
        i += 1
        cv2.imwrite('./faces/' + str(i) + '.png', np.transpose(face, (1,2,0)))
    data['num_pics'] = i
    with open(args.source_data, 'w') as data_file:
        json.dump(data, data_file)


