import vargfacenet

import sklearn
import numpy as np
from tensorflow.keras.preprocessing import image
import tensorflow as tf

# load img with BGRTranspose=True
img = image.load_img('group.png', target_size = (1920, 1080))
img = image.img_to_array(img)
img = img[..., ::-1]

input_data = np.expand_dims(img, 0)

converter = tf.lite.TFLiteConverter.from_saved_model('./vargfacenet_tf')
tflite_model = converter.convert()

open("vargfacenet.tflite", "wb").write(tflite_model)

# Load TFLite model and allocate tensors.
interpreter = tf.lite.Interpreter(model_content=tflite_model)
interpreter.allocate_tensors()

# Get input and output tensors.
input_details = interpreter.get_input_details()
output_details = interpreter.get_output_details()

# Test the TensorFlow Lite model on random input data.
interpreter.set_tensor(input_details[0]['index'], input_data)

interpreter.invoke()

# The function `get_tensor()` returns a copy of the tensor data.
# Use `tensor()` in order to get a pointer to the tensor.
tflite_results = interpreter.get_tensor(output_details[0]['index'])

print(tflite_results)

# # load img with BGRTranspose=True
# img = image.load_img('thao.png', target_size = (112, 112))
# img = image.img_to_array(img, dtype='int32')
# img = img[..., ::-1]

# # print(img)

# input_data = np.expand_dims(img, 0)

# with tf.compat.v1.Session() as sess:
# 	sess.run(tf.compat.v1.global_variables_initializer())
# 	model = tf.keras.Model(inputs=input_tf, outputs=model_tf, name='vargfacenet')
# 	model.summary()
'''
# Save the model.
export_dir = "/tflite/vargfacenet_tf"
tf.saved_model.save(model_tf, export_dir)

# Convert the model.
converter = tf.lite.TFLiteConverter.from_saved_model(export_dir)
tflite_model = converter.convert()

# Load TFLite model and allocate tensors.
interpreter = tf.lite.Interpreter(model_content=tflite_model)
interpreter.allocate_tensors()

# load img with BGRTranspose=True
img = image.load_img('thao.png', target_size = (112, 112))
img = image.img_to_array(img, dtype='int32')
img = img[..., ::-1]

# Get input and output tensors.
input_details = interpreter.get_input_details()
output_details = interpreter.get_output_details()

# Test the TensorFlow Lite model on random input data.
input_shape = input_details[0]['shape']

input_data = np.expand_dims(img, 0)
interpreter.set_tensor(input_details[0]['index'], input_data)

interpreter.invoke()

# The function `get_tensor()` returns a copy of the tensor data.
# Use `tensor()` in order to get a pointer to the tensor.
tflite_results = interpreter.get_tensor(output_details[0]['index'])

# Test the TensorFlow model on random input data.
tf_results = model(tf.constant(input_data))
'''