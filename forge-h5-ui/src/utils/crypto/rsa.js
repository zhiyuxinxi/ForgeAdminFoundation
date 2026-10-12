import { L } from '@/locales'
import JSEncrypt from 'jsencrypt-ext'

function wrapPublicKeyPem(publicKey) {
  if (!publicKey || publicKey.includes('-----BEGIN')) {
    return publicKey
  }
  const lines = publicKey.match(/.{1,64}/g)?.join('\n') || publicKey
  return `-----BEGIN PUBLIC KEY-----\n${lines}\n-----END PUBLIC KEY-----`
}

export function rsaEncrypt(data, publicKey) {
  const encrypt = new JSEncrypt()
  encrypt.setPublicKey(wrapPublicKeyPem(publicKey))
  const encrypted = encrypt.encrypt(String(data))
  if (!encrypted) {
    throw new Error(L("RSA 加密失败"))
  }
  return encrypted
}
