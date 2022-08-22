import jsonpatch
from flask import Flask, request, jsonify
import copy
import base64
import os

app = Flask(__name__)

@app.route('/decorate', methods=['POST'])
def decorate():
    payload = request.get_json()
    req = payload['request']
    source = req['object']
    target = copy.deepcopy(source)

    add_volume(target)
    add_init_container(target)
    tweak_containers(target)

    patch = jsonpatch.JsonPatch.from_diff(source, target)
    print(patch)

    return jsonify({
        'response': {
            'uid': req['uid'],
            'allowed': True,
            'patchType': 'JSONPatch',
            'patch': base64.b64encode(str(patch).encode()).decode(),

        }
    })


def tweak_containers(target):
    containers = target['spec'].get('containers', [])
    for container in containers:
        add_mount(container)
        edit_env(container)


def edit_env(container):
    env = container.get('env', [])
    env.extend([
        {
            'name': 'OTEL_EXPORTER_JAEGER_SERVICE_NAME',
            'valueFrom': {
				'fieldRef' : {
				  'fieldPath': 'metadata.labels[\'' + os.environ['SERVICE_LABEL_NAME'] + '\']'
				}
			}
		}, 
		{
        	'name': 'OTEL_EXPORTER_JAEGER_ENDPOINT',
            'value': os.environ['OTEL_EXPORTER_JAEGER_ENDPOINT']
    	}, 
    	{
           'name': 'MY_POD_UID',
           'valueFrom': {
              'fieldRef': {
                 'fieldPath': 'metadata.uid'
              }
           }
        },
        {
           'name': 'MY_POD_NAME',
           'valueFrom': {
              'fieldRef': {
                 'fieldPath': 'metadata.name'
              }
           }
        },
        {
           'name': 'MY_NODE_NAME',
           'valueFrom': {
              'fieldRef': {
                 'fieldPath': 'spec.nodeName'
              }
           }
        },
        {
           'name': 'SIGNALFX_SPAN_TAGS',
           'value': 'kubernetes_pod_uid:$(MY_POD_UID),kubernetes_pod_name:$(MY_POD_NAME),kubernetes_node_name:$(MY_NODE_NAME)'
        }
     ])
    
    add_java_agent(env)

    container['env'] = env


def add_java_agent(env):
    existing = [e for e in env if e['name'] == 'JAVA_TOOL_OPTIONS']
    if existing:
        existing = existing[0]
        existing['value'] = existing['value'] + ' -javaagent:/mnt/auto-trace/splunk-otel-javaagent-all.jar'
    else:
        env.append({
            'name': 'JAVA_TOOL_OPTIONS',
            'value': ' -javaagent:/mnt/auto-trace/splunk-otel-javaagent-all.jar'
        })


def add_mount(container):
    mounts = container.get('volumeMounts', [])
    mounts.append({
        'mountPath': '/mnt/auto-trace',
        'name': 'auto-trace-mount'
    })
    container['volumeMounts'] = mounts


def add_init_container(target):
    inits = target['spec'].get('initContainers', [])
    inits.append({
        'name': 'autotrace-additions',
        'image': 'busybox',
        'command': ['wget'],
        'args' : [
          '-O',
          '/mnt/shared/splunk-otel-javaagent-all.jar',
          'https://github.com/signalfx/splunk-otel-java/releases/download/v0.3.2/splunk-otel-javaagent-all.jar'
        ],
        'volumeMounts': [{
            'mountPath': '/mnt/shared',
            'name': 'auto-trace-mount'
        }]
    })
    target['spec']['initContainers'] = inits


def add_volume(target):
    volumes = target['spec'].get('volumes', [])
    volumes.append({
        'name': 'auto-trace-mount',
        'emptyDir': {}
    })
    target['spec']['volumes'] = volumes


if __name__ == "__main__":
    app.run('0.0.0.0', debug=False, ssl_context=('webhook-server-tls.crt', 'webhook-server-tls.key'))
