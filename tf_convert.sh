python -m mmdnn.conversion._script.convertToIR -f mxnet -n models/model-symbol.json -w models/model-0001.params -d vargfacenet --inputShape 3,112,112
python -m mmdnn.conversion._script.IRToCode -f tensorflow --IRModelPath vargfacenet.pb --IRWeightPath vargfacenet.npy --dstModelPath vargfacenet_tf.py
mmtomodel -f tensorflow -in vargfacenet.py -iw vargfacenet.npy -o vargfacenet_tf --dump_tag SERVING