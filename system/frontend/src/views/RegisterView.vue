<template>
  <div class="auth-shell">
    <div class="auth-shell__grid">
      <section class="auth-visual auth-visual--register">
        <div class="auth-visual__content">
          <span class="auth-visual__eyebrow">Join ShipInsight</span>
          <h1 class="auth-visual__title">申请加入<br />AIS 态势网络</h1>
          <p class="auth-visual__desc">
            面向船舶运营方与公众观察员开放注册申请。审核通过后，你可以查看 AIS 船舶交通态势、管理船舶档案与航线数据。
          </p>
          <div class="auth-visual__chips">
            <span>船舶运营</span>
            <span>态势观察</span>
            <span>审核准入</span>
          </div>
        </div>

        <div class="auth-visual__footer">
          <div class="auth-metric">
            <strong>审核制</strong>
            <span>提交后进入待审核状态，管理员通过后即可登录系统。</span>
          </div>
          <div class="auth-metric">
            <strong>角色化</strong>
            <span>根据船舶运营方、公众观察员等身份分配不同数据可见范围与操作权限。</span>
          </div>
          <div class="auth-metric">
            <strong>可追踪</strong>
            <span>个人资料、活动日志与角色变化会在系统内持续记录。</span>
          </div>
        </div>
      </section>

      <el-card class="panel-card auth-card" shadow="never">
      <template #header>
        <div class="auth-card__header">
          <span class="auth-card__eyebrow">ShipInsight</span>
          <strong>用户注册申请</strong>
          <p>填写你的基础信息并提交申请，审核通过后即可进入 AIS 船舶交通态势平台。</p>
        </div>
      </template>

      <el-form label-position="top" @submit.prevent="handleSubmit">
        <el-form-item label="申请身份">
          <el-radio-group v-model="form.roleCode">
            <el-radio value="OPERATOR">船舶运营方</el-radio>
            <el-radio value="OBSERVER">公众观察员</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="用户名">
          <el-input v-model="form.username" placeholder="请输入用户名" />
        </el-form-item>
        <el-form-item label="显示名称">
          <el-input v-model="form.displayName" placeholder="请输入显示名称" />
        </el-form-item>
        <el-form-item label="邮箱">
          <el-input v-model="form.email" placeholder="请输入邮箱" />
        </el-form-item>
        <el-form-item label="手机号">
          <el-input v-model="form.phone" placeholder="请输入手机号" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="form.password" type="password" show-password placeholder="请输入密码" />
        </el-form-item>
        <el-form-item label="图形验证码">
          <div class="captcha-row">
            <el-input
              v-model="form.captchaCode"
              class="captcha-input"
              maxlength="5"
              placeholder="请输入验证码"
              @keyup.enter="handleSubmit"
            />
            <button
              class="captcha-image-button"
              type="button"
              :disabled="captchaLoading"
              title="点击刷新验证码"
              @click="loadCaptcha"
            >
              <img v-if="captcha.imageBase64" :src="captcha.imageBase64" alt="图形验证码" />
              <span v-else>{{ captchaLoading ? '加载中' : '刷新' }}</span>
            </button>
            <el-button
              class="captcha-refresh"
              :icon="RefreshRight"
              :loading="captchaLoading"
              circle
              aria-label="刷新验证码"
              @click="loadCaptcha"
            />
          </div>
        </el-form-item>
        <el-button type="primary" size="large" class="auth-card__button" :loading="submitting" @click="handleSubmit">
          提交注册申请
        </el-button>
      </el-form>

      <div class="auth-card__footer">
        <span>提交后需要管理员审核通过才能登录</span>
        <RouterLink to="/login">返回登录</RouterLink>
      </div>
      </el-card>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { RefreshRight } from '@element-plus/icons-vue'
import { RouterLink, useRouter } from 'vue-router'
import { fetchCaptcha, register } from '@/api/auth'

const router = useRouter()
const submitting = ref(false)
const captchaLoading = ref(false)

const form = reactive({
  roleCode: 'OPERATOR' as 'OPERATOR' | 'OBSERVER',
  username: '',
  displayName: '',
  email: '',
  phone: '',
  password: '',
  captchaCode: '',
})

const captcha = reactive({
  captchaId: '',
  imageBase64: '',
})

async function handleSubmit() {
  if (!form.username.trim() || !form.displayName.trim() || !form.password.trim()) {
    ElMessage.warning('请填写完整的注册信息')
    return
  }
  if (!form.captchaCode.trim()) {
    ElMessage.warning('请输入图形验证码')
    return
  }
  if (!captcha.captchaId) {
    ElMessage.warning('验证码加载失败，请刷新验证码后重试')
    await loadCaptcha()
    return
  }

  submitting.value = true
  try {
    await register({
      roleCode: form.roleCode,
      username: form.username.trim(),
      displayName: form.displayName.trim(),
      email: form.email.trim() || undefined,
      phone: form.phone.trim() || undefined,
      password: form.password,
      captchaId: captcha.captchaId,
      captchaCode: form.captchaCode.trim(),
    })
    ElMessage.success('注册申请已提交，请等待管理员审核')
    router.push('/login')
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '注册申请提交失败')
    await loadCaptcha()
  } finally {
    submitting.value = false
  }
}

async function loadCaptcha() {
  captchaLoading.value = true
  try {
    const nextCaptcha = await fetchCaptcha()
    captcha.captchaId = nextCaptcha.captchaId
    captcha.imageBase64 = nextCaptcha.imageBase64
    form.captchaCode = ''
  } catch (error) {
    captcha.captchaId = ''
    captcha.imageBase64 = ''
    ElMessage.error(error instanceof Error ? error.message : '验证码加载失败')
  } finally {
    captchaLoading.value = false
  }
}

onMounted(() => {
  void loadCaptcha()
})
</script>

<style scoped>
.auth-visual--register {
  background-position: center 42%;
}

.captcha-row {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 154px 38px;
  gap: 10px;
  align-items: center;
  width: 100%;
}

.captcha-input {
  min-width: 0;
}

.captcha-image-button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 154px;
  height: 46px;
  padding: 0;
  overflow: hidden;
  border: 1px solid rgba(0, 229, 255, 0.28);
  border-radius: 16px;
  background:
    linear-gradient(135deg, rgba(0, 229, 255, 0.1), rgba(124, 60, 255, 0.08)),
    rgba(4, 14, 36, 0.76);
  color: var(--gsmv-text);
  cursor: pointer;
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.06),
    0 10px 22px rgba(0, 4, 18, 0.2);
}

.captcha-image-button:disabled {
  cursor: wait;
  opacity: 0.72;
}

.captcha-image-button img {
  display: block;
  width: 154px;
  height: 46px;
}

.captcha-refresh {
  width: 38px;
  height: 38px;
}

@media (max-width: 560px) {
  .captcha-row {
    grid-template-columns: minmax(0, 1fr) 42px;
  }

  .captcha-image-button {
    grid-column: 1 / -1;
    width: 100%;
  }

  .captcha-image-button img {
    width: 154px;
  }
}
</style>
