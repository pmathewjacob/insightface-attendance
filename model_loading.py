import vargfacenet

from sklearn import preprocessing
import numpy as np
from six import text_type as _text_type
from tensorflow.keras.preprocessing import image
import tensorflow.compat.v1 as tf

tf.disable_v2_behavior()

# import converted model
model_converted = vargfacenet.KitModel('vargfacenet.npy')
input_tf, model_tf = model_converted

# load img with BGRTranspose=True
img = image.load_img('quan.png', target_size = (112, 112))
img = image.img_to_array(img, dtype='int32')
img = img[..., ::-1]

input_data = np.expand_dims(img, 0)
print(input_data)

img2 = image.load_img('thao.png', target_size = (112, 112))
img2 = image.img_to_array(img2)
img2 = img2[..., ::-1]

input_data2 = np.expand_dims(img2, 0)



# inference with tensorflow
with tf.Session() as sess:
		init = tf.global_variables_initializer()
		sess.run(init)
		feat = sess.run(model_tf, feed_dict = {input_tf : input_data})
		print("----------")
		print(feat)
		print("----------")
		predict = preprocessing.normalize(sess.run(model_tf, feed_dict = {input_tf : input_data}))
		predict2 = preprocessing.normalize(sess.run(model_tf, feed_dict = {input_tf: input_data2}))

diff = np.subtract(predict, predict2)
dist = np.sum(np.square(diff))
print(dist)