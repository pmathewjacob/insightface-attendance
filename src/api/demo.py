import face_model
import cv2
import argparse
import numpy as np
import json
import sklearn

parser = argparse.ArgumentParser(description='do verification')

parser.add_argument('-a', '--add', action="store_true", help='add more faces')
parser.add_argument('-e', '--edit', action="store_true", help='modify data')
parser.add_argument('-c', '--check', action='store_true', help='check attendance')
parser.add_argument('--image-size', default='112,112', help='')
parser.add_argument('--model', default='../../recognition/models/vargfacenet-arcface-retina/model,1', help='path to load model')
parser.add_argument('--gpu', default=0, type=int, help='gpu id')
parser.add_argument('--threshold', default=1.24, type=float, help='ver dist threshold')
parser.add_argument('--source-image', default= None, help='path to source image')
parser.add_argument('--source-data', default='./source.json', help='path to source data')
args = parser.parse_args()


if args.add:
    source_img = cv2.imread(args.source_image)
    if source_img is None:
        print('Source image is invalid')
    else:
        model = face_model.FaceModel(args)
        faces = model.get_all_faces(source_img)
        print('Detected %d faces' % (len(faces)))
        try:
            with open(args.source_data) as data_file:
                data = json.load(data_file)
        except IOError:
            data = dict()
            data['students'] = dict()
            data['num_pics'] = 0
            data['faces_path'] = './faces/'
        for face in faces:
            data['num_pics'] += 1
            cv2.imwrite('./faces/' + str(data['num_pics']) + '.png', np.transpose(face, (1,2,0)))
        with open(args.source_data, 'w') as data_file:
            json.dump(data, data_file, indent=4)
elif args.edit:
    model = face_model.FaceModel(args)
    with open(args.source_data) as data_file:
        data = json.load(data_file)
        students = data['students']
        print('Student ID, Name')
        for student_id in students:
            print('%s, %s' % (student_id, students[student_id]['name']))
    while True:
        command = input('Type y to edit students ')
        if command == 'y':
            name = input('Name: ')
            student_id = input('Student ID: ')
            students[student_id] = dict()
            students[student_id]['name'] = name
            pictures = input('Pictures: ')
            pictures_list = [int(i) for i in pictures.split(',')]
            target_img_list = [cv2.imread(data['faces_path'] + str(picture) + '.png') for picture in pictures_list]
            target_face_list = []
            pp = 0
            for img in target_img_list:
              target_force = False
              if pp==len(target_img_list)-1 and len(target_face_list)==0:
                target_force = True
              target_face = model.get_aligned_face(img, target_force)
              if target_face is not None:
                target_face_list.append(target_face)
              pp+=1
            print('target face', len(target_face_list)) 
            target_feature = None
            for target_face in target_face_list:
              _feature = model.get_feature(target_face, False)
              if target_feature is None:
                target_feature = _feature
              else:
                target_feature += _feature
            target_feature = sklearn.preprocessing.normalize(target_feature)
            students[student_id]['features'] = target_feature.tolist()
        else:
            break
    data['students'] = students
    with open(args.source_data, 'w') as data_file:
        json.dump(data, data_file, indent=4)
elif args.check:
    source_img = cv2.imread(args.source_image)
    if source_img is None:
        print('Source image is invalid')
    else:
        model = face_model.FaceModel(args)
        faces = model.get_all_faces(source_img)
        source_features = np.array([model.get_feature(face, True) for face in faces])
        #print(source_features.shape)
        print('Detected %d faces' % (len(faces)))
        with open(args.source_data) as data_file:
            data = json.load(data_file)
            students = data['students']
            for student_id in students:
                target_feature = np.array(students[student_id]['features'])
                #print(target_feature.shape)
                diff = np.subtract(source_features, target_feature)
                dist = np.sum(np.square(diff), 2)
                print(dist)
                if len(dist[dist<=args.threshold]) > 0:
                    print('%s - %s' % (student_id, students[student_id]['name']))
