import http.server
import json
import smtplib
from email.mime.text import MIMEText
from email.header import Header

SMTP_HOST_NAME = "smtp.office365.com"
SMTP_PORT = 587
SMTP_AUTH_USER = "no-reply@dgy.ac.cn"
SMTP_AUTH_PWD = "dc611963-ac43-4906-a276-a319eb4fb243"


class MyHandler(http.server.BaseHTTPRequestHandler):
    def do_POST(self):
        body = self.rfile.read(int(self.headers['Content-Length']))
        data = json.loads(body)
        if data['uuid'] == '7a095c01-0930-403c-85bb-41bbe10eb89c':
            self.send_response(200)
            send_email(data['receiver'], data['subject'], data['content'])
            self.end_headers()
            self.wfile.write(bytes("OK", "utf-8"))
        else:
            self.send_response(403)
            self.end_headers()
            self.wfile.write(bytes("Forbidden", "utf-8"))


def send_email(recipient, subject, body):
    message = MIMEText(body, 'plain', 'utf-8')
    message['From'] = Header(SMTP_AUTH_USER)
    message['To'] = Header(recipient)
    message['Subject'] = Header(subject)
    smtp = smtplib.SMTP(SMTP_HOST_NAME, SMTP_PORT)
    smtp.starttls()
    smtp.login(SMTP_AUTH_USER, SMTP_AUTH_PWD)
    smtp.sendmail(SMTP_AUTH_USER, recipient, message.as_string())
    smtp.quit()


if __name__ == "__main__":
    server_address = ('', 8090)
    httpd = http.server.HTTPServer(server_address, MyHandler)
    httpd.serve_forever()
