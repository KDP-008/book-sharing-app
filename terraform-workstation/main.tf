resource "aws_vpc" "hackathon_vpc" {
  cidr_block           = "172.31.0.0/16"
  enable_dns_hostnames = true
  enable_dns_support   = true

  tags = {
    Name = "hackathon_vpc"
  }
}

resource "aws_subnet" "hackathon_public_subnet" {
  vpc_id                  = aws_vpc.hackathon_vpc.id
  cidr_block              = "172.31.64.0/20"
  map_public_ip_on_launch = true
  availability_zone       = "us-east-1a"

  tags = {
    Name = "hackathon_public_subnet"
  }
}

resource "aws_internet_gateway" "hackathon_internet_gateway" {
  vpc_id = aws_vpc.hackathon_vpc.id

  tags = {
    Name = "hackathon_internet_gateway"
  }
}

resource "aws_route_table" "hackathon_route_table" {
  vpc_id = aws_vpc.hackathon_vpc.id

  tags = {
    Name = "hackathon_route_table"
  }
}

resource "aws_route" "default_route" {
  route_table_id         = aws_route_table.hackathon_route_table.id
  destination_cidr_block = "0.0.0.0/0"
  gateway_id             = aws_internet_gateway.hackathon_internet_gateway.id
}

resource "aws_route_table_association" "hackathon_route_table_assoc" {
  subnet_id      = aws_subnet.hackathon_public_subnet.id
  route_table_id = aws_route_table.hackathon_route_table.id
}

resource "aws_security_group" "hackathon_sg" {
  name        = "aws_hackathon_sg"
  description = "Hackathon VPC security group"
  vpc_id      = aws_vpc.hackathon_vpc.id

  ingress {
    from_port   = 8080
    to_port     = 8080
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }
  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"] 
  }
}

# resource "aws_key_pair" "hackathon_keypair" {
#   key_name   = "aws_hackathon_keypair"
#   public_key = file("/Users/kdp/Projects/IdeaProjects/book-sharing-app/hackathon_keypair.pub")
# }

resource "aws_instance" "ec2_dev" {
  instance_type                 = "t2.micro"
  ami                           = data.aws_ami.server_ami.id
  vpc_security_group_ids        = [aws_security_group.hackathon_sg.id]
  subnet_id                     = aws_subnet.hackathon_public_subnet.id
  key_name                      = aws_key_pair.hackathon_keypair.id
  user_data                     = file("user_data.sh")  # Provide user data script for application setup
  associate_public_ip_address   = true
  root_block_device {  
    volume_size = 20
  }

  tags = {
    Name = "dev-node"
  }
}
