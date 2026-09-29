# Kubectl exercise

## Exercise 1

1. Create name space `lab`: `kubectl create namespace lab`
2. Write `pod.yml` by hand: Pod `web`, label `app: web`, image `nginx:1.27`, port `80`
3. `apply` -> find IP and node with `get -o wide` : `kubectl apply -f pod1.yml` -> `kubectl get pods -o wide -n lab`
4. Read `describe` output top to bottom. Find the events : `kubectl describe pods -n lab`
5. `exec` into and run `curl localhost` : `kubectl exec -n lab web -- curl localhost:80` / `kubectl exec -n lab -it web -- /bin/sh`
6. `port-forward` to 8080 and open it from machine : `kubectl port-forward -n lab web 8080:80`
7. Delete pod and `get pods` again : `kubectl delete -f pod1.yml` (gone 4ever since there is no relicaset)