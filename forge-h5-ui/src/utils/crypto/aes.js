import CryptoJS from 'crypto-js'

export function aesEncrypt(plainText, key) {
  if (!plainText)
    return plainText

  const keyBytes = CryptoJS.enc.Base64.parse(key)
  const encrypted = CryptoJS.AES.encrypt(plainText, keyBytes, {
    mode: CryptoJS.mode.ECB,
    padding: CryptoJS.pad.Pkcs7,
  })

  return encrypted.toString()
}

export function aesDecrypt(cipherText, key) {
  if (!cipherText)
    return cipherText

  const keyBytes = CryptoJS.enc.Base64.parse(key)
  const decrypted = CryptoJS.AES.decrypt(cipherText, keyBytes, {
    mode: CryptoJS.mode.ECB,
    padding: CryptoJS.pad.Pkcs7,
  })

  return decrypted.toString(CryptoJS.enc.Utf8)
}
