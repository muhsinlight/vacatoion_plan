export function buildTripPayload(form) {
  const body = {
    originCity: form.originCity,
    destinationCity: form.destinationCity,
    startDate: form.startDate,
    endDate: form.endDate,
    adults: Number(form.adults),
    transportMode: form.transportMode,
    accommodationType: form.accommodationType,
    travelStyle: form.travelStyle,
    selectedActivityIds: [...(form.selectedActivityIds || [])],
    includeContingency: false,
  }
  if (form.interests?.length) {
    body.interests = [...form.interests]
  }
  if (form.totalBudget !== '' && form.totalBudget != null) {
    body.totalBudget = Number(form.totalBudget)
  }
  if (form.perPersonBudget !== '' && form.perPersonBudget != null) {
    body.perPersonBudget = Number(form.perPersonBudget)
  }
  if (form.specialNeeds?.trim()) {
    body.specialNeeds = form.specialNeeds.trim()
  }
  if (form.customActivityNames?.length) {
    body.customActivityNames = [...form.customActivityNames]
  }
  return body
}

export function groqContextFromForm(form) {
  return {
    startDate: form.startDate,
    endDate: form.endDate,
    adults: Number(form.adults),
    travelStyle: form.travelStyle,
  }
}

export function travelerSummaryFromForm(form) {
  return `${form.adults} yetişkin`
}
