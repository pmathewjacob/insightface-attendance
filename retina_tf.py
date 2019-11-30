import tensorflow as tf

__weights_dict = dict()

is_train = False

def load_weights(weight_file):
    import numpy as np

    if weight_file == None:
        return

    try:
        weights_dict = np.load(weight_file).item()
    except:
        weights_dict = np.load(weight_file, encoding='bytes').item()

    return weights_dict


def KitModel(weight_file = None):
    global __weights_dict
    __weights_dict = load_weights(weight_file)

    data            = tf.placeholder(tf.float32, shape = (None, 1920, 1080, 3), name = 'data')
    bn_data         = batch_normalization(data, variance_epsilon=1.9999999494757503e-05, name='bn_data')
    conv0_pad       = tf.pad(bn_data, paddings = [[0, 0], [3, 3], [3, 3], [0, 0]])
    conv0           = convolution(conv0_pad, group=1, strides=[2, 2], padding='VALID', name='conv0')
    bn0             = batch_normalization(conv0, variance_epsilon=1.9999999494757503e-05, name='bn0')
    relu0           = tf.nn.relu(bn0, name = 'relu0')
    pooling0_pad    = tf.pad(relu0, paddings = [[0, 0], [1, 1], [1, 1], [0, 0]], constant_values=float('-Inf'))
    pooling0        = tf.nn.max_pool(pooling0_pad, [1, 3, 3, 1], [1, 2, 2, 1], padding='VALID', name='pooling0')
    stage1_unit1_bn1 = batch_normalization(pooling0, variance_epsilon=1.9999999494757503e-05, name='stage1_unit1_bn1')
    stage1_unit1_relu1 = tf.nn.relu(stage1_unit1_bn1, name = 'stage1_unit1_relu1')
    stage1_unit1_conv1 = convolution(stage1_unit1_relu1, group=1, strides=[1, 1], padding='VALID', name='stage1_unit1_conv1')
    stage1_unit1_sc = convolution(stage1_unit1_relu1, group=1, strides=[1, 1], padding='VALID', name='stage1_unit1_sc')
    stage1_unit1_bn2 = batch_normalization(stage1_unit1_conv1, variance_epsilon=1.9999999494757503e-05, name='stage1_unit1_bn2')
    stage1_unit1_relu2 = tf.nn.relu(stage1_unit1_bn2, name = 'stage1_unit1_relu2')
    stage1_unit1_conv2_pad = tf.pad(stage1_unit1_relu2, paddings = [[0, 0], [1, 1], [1, 1], [0, 0]])
    stage1_unit1_conv2 = convolution(stage1_unit1_conv2_pad, group=1, strides=[1, 1], padding='VALID', name='stage1_unit1_conv2')
    stage1_unit1_bn3 = batch_normalization(stage1_unit1_conv2, variance_epsilon=1.9999999494757503e-05, name='stage1_unit1_bn3')
    stage1_unit1_relu3 = tf.nn.relu(stage1_unit1_bn3, name = 'stage1_unit1_relu3')
    stage1_unit1_conv3 = convolution(stage1_unit1_relu3, group=1, strides=[1, 1], padding='VALID', name='stage1_unit1_conv3')
    return data, tf.concat([stage1_unit1_sc, stage1_unit1_conv3], 0)


def convolution(input, name, group, **kwargs):
    w = tf.Variable(__weights_dict[name]['weights'], trainable=is_train, name=name + "_weight")
    if group == 1:
        layer = tf.nn.convolution(input, w, name=name, **kwargs)
    else:
        weight_groups = tf.split(w, num_or_size_splits=group, axis=-1)
        xs = tf.split(input, num_or_size_splits=group, axis=-1)
        convolved = [tf.nn.convolution(x, weight, name=name, **kwargs) for
                    (x, weight) in zip(xs, weight_groups)]
        layer = tf.concat(convolved, axis=-1)

    if 'bias' in __weights_dict[name]:
        b = tf.Variable(__weights_dict[name]['bias'], trainable=is_train, name=name + "_bias")
        layer = layer + b
    return layer

def batch_normalization(input, name, **kwargs):
    mean = tf.Variable(__weights_dict[name]['mean'], name = name + "_mean", trainable = is_train)
    variance = tf.Variable(__weights_dict[name]['var'], name = name + "_var", trainable = is_train)
    offset = tf.Variable(__weights_dict[name]['bias'], name = name + "_bias", trainable = is_train) if 'bias' in __weights_dict[name] else None
    scale = tf.Variable(__weights_dict[name]['scale'], name = name + "_scale", trainable = is_train) if 'scale' in __weights_dict[name] else None
    return tf.nn.batch_normalization(input, mean, variance, offset, scale, name = name, **kwargs)


