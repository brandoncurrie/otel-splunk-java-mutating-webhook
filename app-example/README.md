# Example Application for Auto-Instrumentation with Otel-Java

## Setup with Minikube

Connect your docker profile to minikube (if you haven't already)
```
eval $(minikube docker-env)
```

## Build the application docker image

```
mvn package
docker build -t greeting-service .
```

## Create the Kubernetes Deployment

``` 
kubectl apply -f k8s-greeting-deployment.yaml
```


