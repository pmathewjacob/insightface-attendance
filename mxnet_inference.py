# inference with mxnet

import mxnet as mx
from tensorflow.contrib.keras.api.keras.preprocessing import image
import numpy as np

from collections import namedtuple
Batch = namedtuple('Batch', ['data'])

ctx = mx.gpu()

# load model
sym, arg_params, aux_params = mx.model.load_checkpoint('models/model', 1)
mod = mx.mod.Module(symbol = sym, context= ctx, label_names= None)
mod.bind(for_training=False, data_shapes=[('data', (1, 3, 112, 112))], label_shapes= mod._label_shapes)
mod.set_params(arg_params, aux_params, allow_missing= True)


path = 'thao.png'

# load image with BGRTranspose=True
img = image.load_img(path, target_size = (112, 112))
img = image.img_to_array(img)

img = img[..., ::-1]


# channel first in mxnet
img = np.expand_dims(img, 0).transpose((0,3,1,2))

# compute the predict probabilities
mod.forward(Batch([mx.nd.array(img)]))
prob = mod.get_outputs()[0].asnumpy()
prob = np.squeeze(prob)
print("----------")
print(prob)
print("----------")
print(np.sum(np.square(prob)))