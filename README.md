# book-sharing-app

### Steps to create an EC2 instance & deploy the spring boot jar in AWS:

- Install terraform on your local machine
- The terraform files are already added within [terraform-workstation](terraform-workstation) folder
- The files contains all the setup required to run an EC2 instance, push the jar and run it.
- Create a key in your local machine and update the `main.tf` file to use it.
  - config to change:
    ```
    resource "aws_key_pair" "hackathon_keypair" {
        key_name   = "aws_hackathon_keypair"
        public_key = file("/<path_to_your_key_pair>.pub")
      }
    ```
  - The key is not required I guess if we are using JPMC sandbox otherwise it would have helped to ssh into the instance`
  
- You can also follow this page in case of doubts:
  - https://awstip.com/how-to-deploy-an-aws-ec2-instance-using-terraform-be164e6ac757
