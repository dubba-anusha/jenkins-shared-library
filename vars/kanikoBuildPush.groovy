def call(String imageName, String environment) {
    def jobName = "kaniko-${environment}"
    def workspacePath = env.WORKSPACE.replace('/var/jenkins_home', '/workspace')

    sh """
cat > kaniko-job.yaml <<YAML
apiVersion: batch/v1
kind: Job
metadata:
  name: ${jobName}
spec:
  backoffLimit: 0
  template:
    spec:
      restartPolicy: Never
      containers:
      - name: kaniko
        image: gcr.io/kaniko-project/executor:latest
        args:
        - "--dockerfile=${workspacePath}/Dockerfile"
        - "--context=dir://${workspacePath}"
        - "--destination=docker.io/${imageName}:${environment}"
        volumeMounts:
        - name: docker-config
          mountPath: /kaniko/.docker
        - name: workspace
          mountPath: /workspace
      volumes:
      - name: docker-config
        secret:
          secretName: dockerhub-secret
          items:
          - key: .dockerconfigjson
            path: config.json
      - name: workspace
        persistentVolumeClaim:
          claimName: jenkins-pvc
YAML

/var/jenkins_home/bin/kubectl delete job ${jobName} --ignore-not-found=true
/var/jenkins_home/bin/kubectl apply -f kaniko-job.yaml
/var/jenkins_home/bin/kubectl wait --for=condition=complete job/${jobName} --timeout=300s
/var/jenkins_home/bin/kubectl logs job/${jobName}
"""
}
